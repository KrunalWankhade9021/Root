# Rootd — Design

Date: 2026-10-21 · Status: draft for approval

Rootd is a personal Android focus app in the spirit of Forest: you start a focus session, a tree grows,
and leaving for a distracting app withers it. Rootd adds an LLM in two places: a **gatekeeper** that decides
whether you may briefly use a blocked app mid-session, and a **pattern observer** that turns your history into
weekly insights. It is built for one user (the owner), on their own phone.

Background research on how Forest works is in `docs/forest-re-notes.md` (reverse engineered from Forest
v5.31.0). Use it for behavior reference only.

## 1. Why

Forest gives you two options when you reach for a blocked app: give up (tree dies, session lost) or nothing.
Real needs get punished and weak excuses end the session anyway. Rootd adds a middle path — a small, costly,
time-boxed **pass** granted by an LLM that knows your session goal and your habits — and then learns from those
moments to show you your own patterns.

## 2. Goals, non-goals, anti-goals

**Goals**
- Forest-like core loop: timer/stopwatch, tree grows while focusing, withers on give-up.
- Real enforcement on Android: non-allowed apps are blocked by an overlay during a session.
- LLM gatekeeper for mid-session pass requests (the headline feature).
- Pattern tracking over time with LLM-written weekly insights that **only suggest**.
- Personal use: no accounts, no backend, all data on the phone.

**Non-goals (v1)**
- Friends, group planting, rankings, real-tree donations — need a server.
- Ads, paywall, subscriptions, in-app purchases.
- iOS, tablets, Wear OS, cloud sync.
- LLM-written post-session Reflect messages and AI session planning (discussed; possible later).

**Anti-goals (would make it a failure even if it works)**
- The gatekeeper is slow (> ~3 s typical), preachy/lecturing, or trivially talked past.
- The app changes your settings or strictness on its own based on insights.
- Raw usage history leaves the phone.
- Any Forest code, art, sounds, animations, or the "Forest" name in Rootd.

## 3. Constraints

- **Platform:** Android, Kotlin, Jetpack Compose. Owner's device: Moto g54 5G. minSdk 29 *(my call — matches Forest)*.
- **LLM provider:** Groq (open-source models) via its OpenAI-compatible API, base URL
  `https://api.groq.com/openai/v1`. Base URL, API key and model names are user settings, so any
  OpenAI-compatible provider (OpenRouter, local Ollama) works without a rebuild. The API key is stored on device
  (encrypted storage) — acceptable because the app is personal.
- **Models** *(my call)*: two settings — *gatekeeper model* (small, fast) and *profile model* (larger). Exact Groq
  model IDs are chosen at build time from Groq's current model list.
- **Original assets only:** tree art, sounds, animations must be original or properly licensed (e.g. CC0).
- **Legal:** the Forest notes describe behavior; no decompiled code may be copied.

## 4. Core app (Forest parity, trimmed)

### 4.1 Focus session
- **Timer mode:** choose 10–120 min in 5-min steps. **Stopwatch mode:** counts up to 120 min; stopping before
  10 min gives a withered tree (Forest rule).
- **Cancel window:** within the first **10 seconds**, the user can cancel with no tree and no penalty.
- **Give up:** a confirmation with a short "think for 5 seconds" countdown before the button is enabled;
  giving up withers the tree.
- Each session has a species, an optional tag, and an optional one-line note/goal (used by the gatekeeper).
- One tree per session *(my call; Forest can plant several)*. Visual growth in 4 stages by % of planned
  duration *(my call)*: seed → sprout → sapling → full tree; separate withered look.
- Session state is persisted so it survives the UI being closed or the process restarting; time is computed
  from wall-clock timestamps, not an in-memory counter.
- **App killed by the system:** unlike Forest (which withers the tree), Rootd resumes the session on restart
  *(my call — OEM background kills are common and this is a personal app; enforcement lives in a system-managed
  service, see 4.2, so swiping Rootd away does not escape blocking).*

### 4.2 Blocking ("Deep Focus")
- During a session, if the foreground app is not Rootd and not on the active Allow List, show a full-screen
  overlay over it: "Back to focus" (primary) and "I need it" (secondary → gatekeeper, §5).
- Detection *(my call)*: an AccessibilityService for instant window-change events (it is kept alive by the
  system), with UsageStatsManager polling as a fallback. Overlay via `SYSTEM_ALERT_WINDOW`.
- **Locking the screen or turning it off does not count as leaving.**
- Required permissions are requested in onboarding with plain explanations; Deep Focus is always on in Rootd
  (no "permissions off = leaving doesn't count" mode).
- **Allow Lists:** multiple named lists; pick one per session. An **Always Allowed** set (phone/dialer,
  emergency, maps, SMS/authenticator for OTPs — user editable) never triggers the overlay or the LLM.
- Optional setting: silence notifications during sessions by turning on Do Not Disturb (notification policy
  access) and restoring the previous mode when the session ends *(my call — DND holds notifications instead of
  dismissing them, so nothing is lost)*.

### 4.3 Coins and shop
- Coins for a **successful** session use Forest's formula (minutes `m`):
  `base = (m < 25 ? 1 : 4) + floor(m/5) + floor(max(m−30,0)/30)·5` (e.g. 25→9, 60→21, 120→43).
- **No-pass bonus:** +20% (rounded down) if the session used zero passes.
- **Pass cost:** 2 coins per pass-minute actually used, deducted at session end. Coin balance never goes below 0
  *(my call)*.
- Shop: unlock tree species with coins. Starter species free; prices *(my call)* in the 300–1500 range.
- Ambient sounds (rain, forest, café…) — free, not coin-gated *(my call)*; loop while focusing.

### 4.4 Tags, stats, history
- Tags with name + color; editable later on a session.
- **Dashboard** — one screen with Day / Week / Month / Year tabs, swipe to move between periods:
  - Day: that day's trees on a patch of ground (withered included), focus time, trees, coins, passes, a
    timeline of when you focused, time by tag, session list (tap → note, passes, coins).
  - Week: bar per day, the week's forest, success %, **day streak**, best hours, link to the weekly insight card.
  - Month: calendar heat map (darker = more focus; tap a day → Day view), totals, time by tag. Year: same with
    12 months.
- History list with per-session detail (including passes taken).
- Export all data to JSON/CSV *(my call — the only backup in a no-server app)*.

### 4.5 Widgets
- Home-screen widgets: today's forest/total, and a weekly bar chart.

## 5. LLM gatekeeper ("Ask for a pass")

### 5.1 Flow
1. User opens a blocked app → overlay → taps **"I need it"**.
2. Prompt: *"Why, and for how long?"* — one line, typed or spoken (speech-to-text).
3. App sends the request to the gatekeeper model with the context in 5.3.
4. LLM returns one verdict:
   - **allow** — a pass of 1–5 minutes for that one app;
   - **push_back** — a short counter (e.g. "Can it wait 12 min until your session ends?"); the user may accept
     (back to focus) or make **one** appeal, after which the LLM must answer allow or deny;
   - **deny** — a short, non-preachy line; overlay stays.
5. During a pass the **session clock pauses**. When the pass ends the overlay returns. Switching from the pass app
   to a different blocked app shows the overlay again (passes are per-app).
   *(My calls on pass timing:)* a pass ends when its minutes run out **or** the user returns to Rootd (the
   remaining minutes are forfeited, not resumable); visiting the home screen or Always Allowed apps does not end
   it. Pass time pushes back a timer session's end time; the stopwatch 120-min cap and the coin formula's `m`
   count **focus minutes only** (pass time excluded). `returned_before_expiry` = user came back to Rootd before
   the pass ran out.
6. The tree is marked "took a break" if any pass was used; pass minutes cost coins (§4.3).

### 5.2 Hard rules enforced in code (the LLM cannot override)
- Max **2 passes per session**; max **5 minutes** per pass; the LLM's minutes are clamped to this.
- After the pass limit is reached, "I need it" only offers Give Up.
- Always Allowed apps bypass the gatekeeper entirely.
- **Fallback:** if offline, the API errors, or no answer within ~4 s, grant **one** 2-minute emergency pass
  per session without the LLM (logged as `fallback`); otherwise deny. A fallback pass is a normal pass in every
  other way: it counts toward the 2-pass limit, costs coins, cancels the no-pass bonus and marks the tree
  *(my call)*.
- **Request limits** *(my call, against rephrase-until-yes)*: after a **deny** (or a push_back the user
  accepted), "I need it" is disabled for **3 minutes**; max **4 pass requests per session** (appeals don't count
  as separate requests). Past the limit, "I need it" only offers Give Up.

### 5.3 Context sent to the LLM
Reason text, app label, session tag and note/goal, time elapsed/remaining, passes used this session, today's
pass-request count for that app, and the current **focus profile** summary (§6). No raw history.

### 5.4 Output
Structured JSON, validated by the app:
`{"verdict": "allow" | "push_back" | "deny", "minutes": 0-5, "message": "<= 140 chars", "reason_category": "..."}`.
`reason_category` is one of a fixed list *(my call)*: `work_message`, `personal_message`, `call_or_meeting`,
`lookup_info`, `payment_or_otp`, `entertainment`, `just_checking`, `other`. It lets code group pass reasons for
stats (§6.1) without sending raw reason text in the weekly payload. Invalid output is treated as an API error
(→ fallback rule).

### 5.5 Tone
Brief, friendly, direct; references the user's own goal and patterns ("That's your 4th WhatsApp check today and
the last 3 ran long"). Never moralizes.

## 6. Pattern observer and insights

### 6.1 Layer 1 — stats in code (every session, offline)
Completion rate by hour of day, weekday and tag; typical session length before give-up; pass requests by app,
time and `reason_category`; pass verdicts; whether the user returned before the pass expired.

### 6.2 Layer 2 — weekly LLM profile
- Once a week *(my call: on the first app open ≥7 days after the last profile)*, send the aggregated stats
  (no raw records) to the profile model.
- It returns: a **focus profile** (≤ 120 words, stored locally, reused as gatekeeper context), **3 insights**,
  and **1 suggested change**.
- Shown as a **weekly insight card**. A **pre-session tip** may appear on the start screen when a stored insight
  matches the current time/tag (selected by code, no extra LLM call).
- **Insights only suggest.** Rootd never changes settings, strictness or timers automatically.

## 7. Data (local only)

*(Shape is my call; the builder may refine names/types.)* SQLite via Room; settings in a key-value store.
- **Session** (one row = one tree): id, mode (timer/stopwatch), planned_minutes (null for stopwatch),
  started_at, ended_at, status (ongoing / grown / withered / cancelled), fail_reason (gave_up /
  stopwatch_too_short), focus_minutes (excludes pass time), species_id, tag_id (optional), allow_list_id,
  goal_note, took_break. The ongoing session is persisted continuously.
- **PassRequest** (every ask is logged, allowed or not): id, session_id, app_package, app_label, asked_at,
  reason_text (never leaves the phone after the gatekeeper call), reason_category, verdict, was_appeal,
  llm_message, source (llm / fallback), granted_minutes, pass_started_at, pass_ended_at, used_minutes,
  returned_before_expiry.
- **CoinTransaction** (ledger): id, at, amount (±), kind (session_reward / no_pass_bonus / pass_cost /
  species_unlock), session_id?, species_id?. Balance = sum, floored at 0.
- **Tag:** id, name, color, archived. **Species:** id, name, art_key, price_coins, unlocked_at.
- **AllowList:** id, name, is_default. **AllowListApp:** allow_list_id, app_package.
  **AlwaysAllowedApp:** app_package, reason (phone / maps / otp / user).
- **FocusProfile** (one per week): id, generated_at, period_start, period_end, profile_text, insights (3),
  suggestion (1), stats_snapshot (exactly what was sent), seen_at.
- **Settings:** llm_base_url, llm_api_key (encrypted), gatekeeper_model, profile_model, default_mode,
  default_minutes, default_species, default_allow_list, dnd_enabled, ambient_sound.
- **Calculated, not stored:** coin balance, streak, period totals, best hours, success rates.

## 8. Settings

LLM base URL / API key / two model names with a **Test connection** button; Allow Lists and Always Allowed apps;
notification muting; default mode and duration; data export.

## 9. Success measures (visible in stats, for the owner)

Session completion rate; share of passes where the user returned before expiry; passes per session trending
down over weeks.

## 10. Left to the builder

App architecture and module layout, DI and libraries, UI visual design, the tree art/animation approach,
exact Groq model IDs, prompt wording, exact stats chart types, and build/test tooling.
