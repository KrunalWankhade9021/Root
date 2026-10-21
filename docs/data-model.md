# Rootd — Data Model (source of truth)

Version: 1 (database schema version 1) · Date: 2026-10-21
Related: `docs/superpowers/specs/2026-10-21-rootd-design.md` (product design), `docs/forest-re-notes.md` (research)

> **Rule for every contributor (human or AI agent):** this file is the single source of truth for stored data,
> enums, JSON payloads and derived calculations. Do not add, rename or retype a table, column, enum value or
> JSON field in code without first changing this file in the same change, bumping the schema version, and
> writing a migration (§10). If code and this file disagree, this file wins and the code is a bug.

---

## 1. Conventions

| Topic | Rule |
|---|---|
| Storage | SQLite via Room (tables §3–§8). Settings in Jetpack DataStore (§9). Secrets in encrypted storage. |
| Table names | `snake_case`, plural (`sessions`). Kotlin entity: `PascalCase` singular + `Entity` (`SessionEntity`). |
| Column names | `snake_case`. Kotlin properties: `camelCase` mapped with `@ColumnInfo(name = ...)`. |
| Primary keys | `id INTEGER PRIMARY KEY AUTOINCREMENT` (Kotlin `Long`) unless stated. |
| Timestamps | `INTEGER` epoch **milliseconds, UTC** (Kotlin `Long`, convert with `java.time.Instant`). Suffix `_at`. |
| Durations | `INTEGER` **seconds** unless the name says `_minutes`. Suffix `_seconds` / `_minutes`. |
| Local dates | `TEXT` ISO `yyyy-MM-dd` in the **device time zone at the moment of the event**. Suffix `_local_date`. |
| Booleans | `INTEGER` 0/1, `NOT NULL`, explicit default. Kotlin `Boolean`. |
| Enums | `TEXT` holding the exact lowercase value listed in §2. Kotlin `enum class` + Room `TypeConverter`. Unknown value read from DB → treat as data corruption, log, never crash the blocker. |
| JSON columns | `TEXT` containing JSON that matches a schema in §11, always with a `schema_version` field. |
| Foreign keys | Declared with Room `@ForeignKey`; `PRAGMA foreign_keys=ON` (Room default). Every FK column is indexed. |
| Money/coins | `INTEGER` whole coins. No fractions anywhere. |
| Nullability | `NOT NULL` unless the column is marked *nullable* below. |

---

## 2. Enums (exact stored values)

| Enum | Values | Notes |
|---|---|---|
| `SessionMode` | `timer`, `stopwatch` | |
| `SessionStatus` | `ongoing`, `grown`, `withered`, `cancelled` | At most **one** `ongoing` row at any time (§3.1 invariant). |
| `FailReason` | `gave_up`, `stopwatch_too_short` | Only set when status = `withered`. |
| `Verdict` | `allow`, `push_back`, `deny` | What was decided for a request. |
| `VerdictSource` | `llm`, `fallback` | `fallback` = decided by code because the LLM was unavailable (§12.4). |
| `ReasonCategory` | `work_message`, `personal_message`, `call_or_meeting`, `lookup_info`, `payment_or_otp`, `entertainment`, `just_checking`, `other` | Returned by the LLM; `other` if missing/invalid or fallback. |
| `PassEndReason` | `expired`, `returned_to_rootd`, `session_ended` | Why an active pass stopped. |
| `CoinKind` | `session_reward`, `no_pass_bonus`, `pass_cost`, `species_unlock` | Sign rules in §6. |
| `AlwaysAllowedReason` | `phone`, `maps`, `otp`, `user` | |

---

## 3. `sessions` — one row = one focus session = one tree

| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `id` | INTEGER PK | no | auto | |
| `mode` | TEXT `SessionMode` | no | | |
| `planned_minutes` | INTEGER | **yes** | null | Timer: 10–120, multiple of 5. Stopwatch: null. |
| `started_at` | INTEGER ms | no | | When "Plant" was tapped. |
| `started_local_date` | TEXT date | no | | Local date at `started_at`. Used for all per-day grouping. |
| `ended_at` | INTEGER ms | **yes** | null | Null while `ongoing`. Set for every terminal status. |
| `status` | TEXT `SessionStatus` | no | `ongoing` | |
| `fail_reason` | TEXT `FailReason` | **yes** | null | Non-null iff `status = withered`. |
| `paused_seconds` | INTEGER | no | 0 | Total time spent on passes (clock paused). |
| `pause_started_at` | INTEGER ms | **yes** | null | Non-null only while a pass is active right now. |
| `focus_seconds` | INTEGER | no | 0 | Final focused time, pass time excluded. Written when the session ends (and checkpointed while ongoing). |
| `species_id` | INTEGER FK → `species.id` | no | | `ON DELETE RESTRICT` (species are never deleted). |
| `tag_id` | INTEGER FK → `tags.id` | **yes** | null | `ON DELETE SET NULL` (tags are archived, not deleted, in normal UI). |
| `allow_list_id` | INTEGER FK → `allow_lists.id` | **yes** | null | List used for this session. `ON DELETE SET NULL`. |
| `goal_note` | TEXT | **yes** | null | ≤ 120 chars, one line. Editable after the session. |
| `took_break` | INTEGER bool | no | 0 | 1 if any pass was actually started in this session. |
| `pass_count` | INTEGER | no | 0 | Passes **started** (0–2). Denormalized counter for the hard limit. |
| `request_count` | INTEGER | no | 0 | Non-appeal pass requests made (0–4). Denormalized counter for the hard limit. |
| `last_denied_at` | INTEGER ms | **yes** | null | Last deny / accepted push_back; drives the 3-min cooldown. |

### 3.1 Invariants (enforce in code; test them)
1. At most one row with `status = ongoing` (enforced by a partial unique index, §8).
2. `status = ongoing` ⇔ `ended_at IS NULL`.
3. `fail_reason IS NOT NULL` ⇔ `status = withered`.
4. `pause_started_at IS NOT NULL` ⇒ `status = ongoing` and exactly one `pass_requests` row for this session has an active pass (`pass_started_at` set, `pass_ended_at` null).
5. `pass_count ≤ 2`, `request_count ≤ 4`.
6. `took_break = 1` ⇔ `pass_count ≥ 1`.
7. `status = cancelled` ⇒ `ended_at − started_at < 10 000 ms` (10-second cancel window) and no coin transactions reference the session.

### 3.2 Derived timing (never stored separately)
- **Elapsed focus while ongoing** = `now − started_at − paused_seconds·1000 − (pause_started_at ? now − pause_started_at : 0)`.
- **Timer end time** = `started_at + planned_minutes·60 000 + paused_seconds·1000` (+ current pause if active).
- **Stopwatch cap**: auto-finish when elapsed focus reaches 120 min.
- **Focus minutes for coins** `m` = `floor(focus_seconds / 60)`.
- **Growth stage** (UI only) = by elapsed focus ÷ planned (timer) or ÷ 120 min (stopwatch): `<25%` seed, `<50%` sprout, `<75%` sapling, else tree.

### 3.3 Status transitions
```
(new) ──Plant──▶ ongoing
ongoing ──cancel within 10 s──────────────▶ cancelled
ongoing ──timer reaches end───────────────▶ grown
ongoing ──stopwatch stopped, m ≥ 10───────▶ grown
ongoing ──stopwatch stopped, m < 10───────▶ withered (stopwatch_too_short)
ongoing ──stopwatch hits 120 min──────────▶ grown
ongoing ──Give up confirmed───────────────▶ withered (gave_up)
```
Terminal statuses never change again (except editing `tag_id` / `goal_note`).
On process restart with an `ongoing` row: recompute timing from §3.2 and resume; if the timer end is already in
the past, finish as `grown` at the computed end time.

---

## 4. `pass_requests` — every press of "I need it" (and every appeal)

| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `id` | INTEGER PK | no | auto | |
| `session_id` | INTEGER FK → `sessions.id` | no | | `ON DELETE CASCADE`. |
| `app_package` | TEXT | no | | e.g. `com.instagram.android`. |
| `app_label` | TEXT | no | | User-visible name at request time. |
| `asked_at` | INTEGER ms | no | | |
| `asked_local_date` | TEXT date | no | | For "today's requests for this app". |
| `reason_text` | TEXT | no | | What the user typed/said, ≤ 300 chars. **Never included in weekly payloads.** |
| `reason_category` | TEXT `ReasonCategory` | no | `other` | From the LLM; `other` for fallback/invalid. |
| `was_appeal` | INTEGER bool | no | 0 | 1 if this row is the single appeal after a push_back. |
| `original_request_id` | INTEGER FK → `pass_requests.id` | **yes** | null | Set iff `was_appeal = 1`; points to the `push_back` row. `ON DELETE CASCADE`. |
| `verdict` | TEXT `Verdict` | no | | |
| `verdict_source` | TEXT `VerdictSource` | no | | |
| `llm_message` | TEXT | **yes** | null | ≤ 140 chars shown to the user. Fallback rows get a fixed local message. |
| `llm_model` | TEXT | **yes** | null | Model ID used (null for fallback). |
| `llm_latency_ms` | INTEGER | **yes** | null | Round-trip time; null for fallback. |
| `granted_minutes` | INTEGER | no | 0 | 1–5 when `verdict = allow` (after clamping), else 0. |
| `pass_started_at` | INTEGER ms | **yes** | null | Set when the user taps "Start pass". An allowed pass may be declined → stays null. |
| `pass_ended_at` | INTEGER ms | **yes** | null | |
| `pass_end_reason` | TEXT `PassEndReason` | **yes** | null | Set with `pass_ended_at`. |
| `used_seconds` | INTEGER | no | 0 | `pass_ended_at − pass_started_at`, capped at `granted_minutes·60`. |
| `returned_before_expiry` | INTEGER bool | **yes** | null | Null if no pass started; else 1 iff `pass_end_reason = returned_to_rootd`. |

`pass_end_reason` rules: `expired` = the granted time ran out; `returned_to_rootd` = the user opened Rootd before
expiry; `session_ended` = the session reached **any** terminal status (grown / withered / cancelled) while the
pass was active, whichever way it ended. These are the only three cases.

### 4.1 Invariants
1. `was_appeal = 1` ⇔ `original_request_id IS NOT NULL`; the referenced row has `verdict = push_back` and the same `session_id`.
2. A `push_back` row has at most one appeal referencing it.
3. An appeal row's verdict is `allow` or `deny` (never `push_back`) — code overrides an LLM `push_back` on appeal to `deny`.
4. `granted_minutes > 0` ⇔ `verdict = allow`.
5. Per session: `count(pass_started_at IS NOT NULL) ≤ 2`; `count(was_appeal = 0) ≤ 4`.
6. Per session: at most one row with `verdict_source = fallback AND verdict = allow`.
7. At most one active pass (started, not ended) per session.

---

## 5. Catalog tables

### 5.1 `tags`
| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `id` | INTEGER PK | no | auto | |
| `name` | TEXT | no | | Unique among non-archived tags (case-insensitive), ≤ 24 chars. |
| `color_argb` | INTEGER | no | | ARGB int, e.g. `0xFF4A7FD1`. |
| `archived` | INTEGER bool | no | 0 | Archived tags are hidden from pickers but kept for history. |
| `created_at` | INTEGER ms | no | | |

Seed: `study`, `work`, `read`, `exercise` (user can edit/archive).

### 5.2 `species`
| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `id` | INTEGER PK | no | | Fixed IDs from seed (not autoincrement). |
| `key` | TEXT UNIQUE | no | | Stable code key, e.g. `oak`. Art files resolved from this. |
| `name` | TEXT | no | | Display name. |
| `price_coins` | INTEGER | no | | 0 for starter species. |
| `unlocked_at` | INTEGER ms | **yes** | null | Null = locked. Starter species are unlocked at install. |
| `sort_order` | INTEGER | no | | Shop order. |

Seed (prices are tunable; final list chosen with the art): `oak` 0 (starter), `pine` 0 (starter),
`birch` 300, `maple` 500, `cherry` 800, `bamboo` 1000, `baobab` 1500.

### 5.3 `allow_lists`
| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `id` | INTEGER PK | no | auto | |
| `name` | TEXT | no | | ≤ 32 chars, unique (case-insensitive). |
| `is_default` | INTEGER bool | no | 0 | Exactly one row has 1 (preselected on the start screen). |
| `created_at` | INTEGER ms | no | | |

Cannot be deleted while referenced by the `ongoing` session. Seed: one empty list "Default".

### 5.4 `allow_list_apps`
| Column | Type | Null | Description |
|---|---|---|---|
| `allow_list_id` | INTEGER FK → `allow_lists.id` | no | `ON DELETE CASCADE` |
| `app_package` | TEXT | no | |

PK: (`allow_list_id`, `app_package`).

### 5.5 `always_allowed_apps`
| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `app_package` | TEXT PK | no | | |
| `reason` | TEXT `AlwaysAllowedReason` | no | | |
| `is_system_default` | INTEGER bool | no | 0 | Seeded rows = 1 and cannot be removed in the UI. |

Seed at first launch (resolved on device, not hard-coded, because OEMs differ), in this order; each step
inserts with `is_system_default = 1` and skips silently if it resolves to nothing or a duplicate:
1. **Dialer** (`phone`): `TelecomManager.defaultDialerPackage`; if null, the package resolving
   `Intent(ACTION_DIAL)`.
2. **SMS / OTP** (`otp`): `Telephony.Sms.getDefaultSmsPackage(context)`; if null, the package resolving
   `Intent(ACTION_SENDTO, Uri.parse("smsto:"))`.
3. **Maps** (`maps`): the package resolving `Intent(ACTION_VIEW, Uri.parse("geo:0,0?q="))`, ignoring the
   system chooser (`android` / resolver activity); if ambiguous, `com.google.android.apps.maps` if installed.

Re-run steps 1–3 when the default dialer/SMS app changes (checked on app start); never remove a seeded row
automatically. Rootd itself is always allowed implicitly (not stored). System UI, the launcher, the input
method (keyboard) and the lock screen are never blocked (handled in code, not stored).

---

## 6. `coin_transactions` — the coin ledger

| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `id` | INTEGER PK | no | auto | |
| `at` | INTEGER ms | no | | |
| `amount` | INTEGER | no | | Signed. Never 0. |
| `kind` | TEXT `CoinKind` | no | | |
| `session_id` | INTEGER FK → `sessions.id` | **yes** | null | Required for `session_reward`, `no_pass_bonus`, `pass_cost`. `ON DELETE SET NULL`. |
| `species_id` | INTEGER FK → `species.id` | **yes** | null | Required for `species_unlock`. |
| `note` | TEXT | **yes** | null | e.g. "clamped from −8 to −3". |

### 6.1 Sign rules
`session_reward` > 0 · `no_pass_bonus` > 0 · `pass_cost` < 0 · `species_unlock` < 0.

### 6.2 Balance
`balance = SUM(amount)`. It must never go below 0. When a `pass_cost` would push it below 0, write the clamped
amount (−balance) and put the original in `note`; if balance is 0, write no row. `species_unlock` requires
`balance ≥ price` (otherwise the purchase is refused).

### 6.3 Writing coins when a session ends (one DB transaction)
For `status = grown` only (withered/cancelled earn nothing and are not charged):
1. `m = floor(focus_seconds / 60)`
2. `base = (m < 25 ? 1 : 4) + floor(m / 5) + floor(max(m − 30, 0) / 30) · 5`
3. Write `session_reward = base`.
4. If `pass_count = 0`: write `no_pass_bonus = floor(base · 0.2)` (skip if 0).
5. Pass cost: `cost = 2 · Σ ceil(used_seconds / 60)` over the session's passes; if > 0 write `pass_cost = −cost` (clamped per §6.2).

For `status = withered` with passes used: still write the `pass_cost` (the pass was used) — clamped per §6.2.

Reference values for `base`: 10→3, 25→9, 30→10, 45→13, 60→21, 90→32, 120→43.

---

## 7. `focus_profiles` — weekly LLM profile and insights

| Column | Type | Null | Default | Description |
|---|---|---|---|---|
| `id` | INTEGER PK | no | auto | |
| `generated_at` | INTEGER ms | no | | |
| `period_start_local_date` | TEXT date | no | | First day covered (inclusive). |
| `period_end_local_date` | TEXT date | no | | Last day covered (inclusive). |
| `profile_text` | TEXT | no | | ≤ 120 words. Used as gatekeeper context (§12.2). |
| `insights_json` | TEXT JSON | no | | Schema §11.3. Exactly 3 insights + 1 suggestion. |
| `stats_snapshot_json` | TEXT JSON | no | | Schema §11.2 — exactly what was sent to the LLM. |
| `llm_model` | TEXT | no | | |
| `seen_at` | INTEGER ms | **yes** | null | When the user opened the weekly card. |

The **latest** row by `generated_at` is the active profile.

**Generation trigger (local dates only, never millisecond arithmetic):** on app open, let `today` = current
local date.
- If a profile exists: generate when `today ≥ latest.period_end_local_date + 8 days`; the new period is
  `latest.period_end_local_date + 1 day` … `today − 1 day` (all complete days; normally 7, longer if the app
  wasn't opened).
- If none exists: generate when ≥ 5 finished sessions exist; the period is the earliest
  `started_local_date` … `today − 1 day`.
- Today is never included (it isn't complete). Continuity: the snapshot's `previous_profile_text` carries last
  week's `profile_text` so the new insights build on prior observations.

---

## 8. Indexes

| Table | Index | Purpose |
|---|---|---|
| `sessions` | `(started_local_date)` | Dashboard per-day / heat map |
| `sessions` | `(started_at)` | Range queries, history list |
| `sessions` | UNIQUE partial `(status) WHERE status = 'ongoing'` | Invariant §3.1-1 (Room can't declare partial indexes — see SQL below) |
| `sessions` | `(tag_id)`, `(species_id)`, `(allow_list_id)` | FK indexes |
| `pass_requests` | `(session_id, asked_at)` | Per-session limits and lists |
| `pass_requests` | `(app_package, asked_local_date)` | "Today's requests for this app" |
| `pass_requests` | `(original_request_id)` | Appeal lookup |
| `coin_transactions` | `(session_id)`, `(at)` | Session coin breakdown, history |
| `allow_list_apps` | PK `(allow_list_id, app_package)` | **Hot path:** "is this app allowed right now?" |
| `always_allowed_apps` | PK `(app_package)` | **Hot path** |
| `focus_profiles` | `(generated_at)` | Latest profile |

The partial index is created with this exact SQL in `RoomDatabase.Callback.onCreate` (fresh installs) **and**
re-asserted in `onOpen` (idempotent, protects against it ever being missing); any future migration that
rebuilds `sessions` must recreate it:
```sql
CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_single_ongoing
  ON sessions(status) WHERE status = 'ongoing';
```

The blocker must not query the DB on every window event: cache the active allow set (allow list apps ∪ always
allowed) in memory when a session starts/changes, and refresh on edits.

---

## 9. Settings (DataStore, not Room)

| Key | Type | Default | Notes |
|---|---|---|---|
| `llm_base_url` | String | `https://api.groq.com/openai/v1` | Any OpenAI-compatible endpoint. |
| `llm_api_key` | String | empty | **Encrypted storage** (Android Keystore-backed). Never logged, never exported. |
| `gatekeeper_model` | String | empty → set during onboarding | Small, fast Groq model ID. |
| `profile_model` | String | empty → set during onboarding | Larger Groq model ID. |
| `llm_timeout_ms` | Int | 4000 | Gatekeeper timeout before fallback. |
| `default_mode` | String `SessionMode` | `timer` | |
| `default_minutes` | Int | 25 | 10–120 step 5. |
| `default_species_id` | Long | id of `oak` | |
| `default_allow_list_id` | Long | id of "Default" | Mirrors `allow_lists.is_default`. |
| `dnd_enabled` | Boolean | false | Turn on Do Not Disturb during sessions. |
| `dnd_previous_filter` | Int | -1 | Saved interruption filter to restore after a session (-1 = none saved). |
| `ambient_sound` | String | `none` | Sound key. |
| `onboarding_done` | Boolean | false | |

---

## 10. Versioning and migrations

- Schema version starts at **1**. Every change to §3–§8 bumps it and adds a Room `Migration(n, n+1)`.
- **Never** use `fallbackToDestructiveMigration` — history must survive updates.
- Room schema export (`exportSchema = true`) is committed so migrations can be tested.
- Every migration gets an instrumented test (`MigrationTestHelper`) from the previous version.
- JSON payloads carry `schema_version`; readers must handle all versions ever written.
- Change log of this file lives at the bottom (§14).

---

## 11. JSON schemas

### 11.1 Gatekeeper LLM response (validated by code)
```json
{
  "schema_version": 1,
  "verdict": "allow | push_back | deny",
  "minutes": 0,
  "message": "string, ≤ 140 chars",
  "reason_category": "work_message | personal_message | call_or_meeting | lookup_info | payment_or_otp | entertainment | just_checking | other"
}
```
Validation: unknown `verdict` → invalid (fallback path). `minutes` clamped to 1–5 when `allow`, forced 0 otherwise.
`message` truncated to 140. Unknown `reason_category` → `other`. On an appeal, `push_back` → treated as `deny`.

### 11.2 Weekly stats snapshot (sent to the profile model; stored in `stats_snapshot_json`)
Aggregates only — **no** `reason_text`, notes, or per-session rows.
```json
{
  "schema_version": 1,
  "period": { "start": "2026-10-12", "end": "2026-10-18" },
  "totals": { "sessions": 14, "grown": 12, "withered": 2, "cancelled": 1, "focus_minutes": 400 },
  "by_hour": [ { "hour": 9, "sessions": 4, "success_rate": 1.0 } ],
  "by_weekday": [ { "weekday": "mon", "focus_minutes": 55, "success_rate": 0.8 } ],
  "by_tag": [ { "tag": "study", "sessions": 8, "success_rate": 0.75, "avg_minutes": 38 } ],
  "length_buckets": [ { "bucket": "45_plus", "sessions": 5, "success_rate": 0.4 } ],
  "give_up_after_minutes_median": 22,
  "passes": {
    "requests": 9, "allowed": 5, "push_back": 2, "denied": 2, "fallback": 0,
    "by_app": [ { "app": "WhatsApp", "requests": 5, "allowed": 3 } ],
    "by_category": { "work_message": 2, "just_checking": 4, "other": 3 },
    "by_hour": [ { "hour": 15, "requests": 4 } ],
    "returned_before_expiry_rate": 0.73
  },
  "previous_profile_text": "string or null"
}
```
Buckets: `under_25`, `25_44`, `45_plus`. App names are labels, not package names.

### 11.3 Insights payload (profile model response; stored in `insights_json`)
```json
{
  "schema_version": 1,
  "profile_text": "≤ 120 words",
  "insights": [
    { "id": "best_window", "text": "≤ 120 chars", "match": { "hours": [9, 10], "tags": [] } },
    { "id": "...", "text": "...", "match": null },
    { "id": "...", "text": "...", "match": null }
  ],
  "suggestion": { "text": "≤ 160 chars" }
}
```
Exactly 3 insights. `match` (optional) lets code pick a **pre-session tip** without another LLM call: show the
insight whose `hours` contains the current hour and/or whose `tags` contains the selected tag.

---

## 12. LLM request context (what leaves the phone)

### 12.1 Gatekeeper request (per "I need it")
Sent: reason text (this request only), app label, session tag name, goal note, minutes elapsed and remaining,
passes used / remaining, requests left, whether this is an appeal (+ the push_back message), today's request
count for this app, and the active `profile_text` (or null). **Not sent:** history rows, other reasons, package
lists, identifiers.

### 12.2 Profile request (weekly)
Sent: the §11.2 snapshot only.

### 12.3 Logging
Request/response bodies are not written to logs. `pass_requests` keeps the user's own reason locally.

### 12.4 Fallback (no LLM decision)
Triggered by: no network, HTTP error, timeout (`llm_timeout_ms`), or invalid JSON. If this session has no
fallback allow yet and `pass_count < 2` → `verdict = allow`, `granted_minutes = 2`, `verdict_source = fallback`.
Otherwise → `verdict = deny`, `verdict_source = fallback`. Fallback passes count toward every limit and cost.

---

## 13. Derived values (computed, never stored)

| Value | Definition |
|---|---|
| Coin balance | `SUM(coin_transactions.amount)` |
| Day focus time | `SUM(focus_seconds)` of sessions with `started_local_date = D` and status in (`grown`,`withered`) |
| Success rate | `grown / (grown + withered)` — `cancelled` excluded |
| Streak | Consecutive local dates ending today (or yesterday if today has none yet) with ≥ 1 `grown` session |
| Best hours | Hours (by `started_at` in local time) with ≥ 3 sessions in the period, ranked by success rate |
| Trees in a period | Sessions with status `grown` or `withered` (cancelled sessions have no tree) |
| Gatekeeper latency | p50 / p95 of `pass_requests.llm_latency_ms` (`verdict_source = llm`) over the last 7 local days; shown in Settings next to the gatekeeper model to help decide when to swap models. Not sent to the LLM. |
| Fallback rate | share of the last 7 days' requests with `verdict_source = fallback`; shown with latency. |

---

## 14. Change log

| Schema | Date | Change |
|---|---|---|
| 1 | 2026-10-21 | Initial model. Review round (Nemotron): clarified `pass_end_reason`, always-allowed seed order, profile trigger on local dates, partial-index SQL, latency/fallback diagnostics. |
