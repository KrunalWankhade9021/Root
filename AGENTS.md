# AGENTS.md — rules for every agent working on Rootd

Two AI agents build this repo — **Claude Code** and **opencode (Nemotron)** — with the owner as reviewer and
merger. Read this file before doing anything. If an instruction here conflicts with something an agent
"remembers", this file wins.

## What Rootd is

A personal Android focus app in the spirit of Forest (plant a tree, it withers if you give up) plus an LLM
gatekeeper for mid-session "pass" requests and weekly LLM insights. One user, no backend, all data on the phone.

## Sources of truth (read before coding)

| Document | Decides |
|---|---|
| `docs/superpowers/specs/2026-10-21-rootd-design.md` | Product behavior, rules, limits, UX flow |
| `docs/data-model.md` | Every table, column, enum value, invariant, coin formula, JSON payload |
| `docs/superpowers/plans/2026-10-21-rootd-build-plan.md` | Task list, owners, dependencies, acceptance tests, progress tracker |
| `docs/forest-re-notes.md` | Research on Forest only — **behavior reference, never code to copy** |

If code and a doc disagree, the doc wins and the code is a bug. To change behavior or data: change the doc
first (in the same branch), then the code.

## Ownership

Every task in the build plan has exactly one owner (`claude` or `opencode`). Work only on your own tasks and
only in the files the task lists. If you need a change in a file owned by the other agent's task, stop and
write it up for the owner instead of editing it. Shared boundaries are Kotlin interfaces defined in an early
plan task; build against the interface, not the other side's implementation.

## Git workflow

- Never commit directly to `main`. One branch per task: `task/<number>-<short-name>` (e.g. `task/07-coin-writer`).
- Small, focused commits with clear messages. Don't mix unrelated changes.
- **Agents never merge their own branches and never push to `main`.** The owner merges after review.
- Before starting a task: update from `main`. Before handing off: rebase/merge `main` and re-run the tests.
- Never rewrite published history (`push --force` to shared branches is not allowed).

## Definition of done (every task)

1. The task's acceptance tests exist and pass: `./gradlew test` (and `connectedAndroidTest` where the task says so).
2. Invariants from `docs/data-model.md` touched by the task have tests.
3. No new warnings ignored, no TODOs left without a plan task.
4. Docs updated if behavior or data changed (and `docs/data-model.md` change log + schema version + migration if the schema changed).
5. The task is checked off in the plan with a 2–3 line handoff note (what was done, anything the next task needs to know).

## Code conventions

- Kotlin + Jetpack Compose. Follow the naming rules in `docs/data-model.md` §1 for anything stored.
- Match the surrounding code's style; don't reformat files you aren't changing.
- Time: inject a `Clock` (no direct `System.currentTimeMillis()` in logic) so time-based rules are testable.
- No network calls except the LLM client; the LLM client is the only place that knows the provider API.

## Secrets and privacy

- The Groq/LLM API key is never committed, logged, printed, or put in test fixtures. Local testing reads it from
  `local.properties` (git-ignored); the app stores it in encrypted settings.
- Only the data listed in `docs/data-model.md` §12 may be sent to the LLM. Never send `reason_text` history,
  notes, or package lists in weekly payloads.

## Legal

Do not copy code, art, sounds, animations, strings or the name from Forest (`cc.forestapp`) or any decompiled
source. Never commit APKs or decompiled output. Rootd's assets are original or properly licensed (e.g. CC0),
with the license noted next to the asset.

## Reviews

Each agent reviews the other's branch before the owner merges. Reviews check: matches the docs, tests prove the
behavior, invariants hold, no secret/privacy leaks, no files outside the task's scope.
