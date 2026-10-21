# Forest (cc.forestapp) — reverse-engineering notes

Research notes for building our own focus app. Behavior only — we write our own code and art.

- Target: Forest v5.31.0 (versionCode 53100000), minSdk 29, targetSdk 36
- Source: pulled from own device (moto g54 5G) via adb; splits: base, arm64_v8a, xxhdpi
- base.apk sha256: 14006b3f31c60ce878b52c8c5099683fc5c87f2f22c42ed6a0ec4d952d787587
- REA evidence: resources/manifest decode `ev_1cd3c2d7ff0fb86689c00837bcb742aba825a4d0a40630faeffb5f685c991916` (strings coverage partial: first 2000 of default table)

## Tech stack (observed from APK contents)

- Native Kotlin, 9 dex files (~90 MB code); mixed XML views (860+ layouts) and Jetpack Compose
- Compose Multiplatform shared module `com.seekrtech.stforestkit` (likely shared with iOS)
- Internal UI/navigation framework `com.seekrtech.truss` (Compose host activities)
- Rive animations (`librive-android.so`, 18 `.riv`), 2230 `.webp` images, 40 ambient `.m4a` loops
- Protobuf (14 `.proto`), Jetpack DataStore, Fresco, Firebase Messaging
- Monetization: Google Play Billing, AppLovin ads (rewarded "watch ad for 2x coins"), Intercom support

## Focus enforcement (observed from manifest + strings; mechanism details need code)

| Mechanism | Evidence |
|---|---|
| Detect foreground app | `PACKAGE_USAGE_STATS`, `GET_TASKS`, `ForestAccessibilityService` |
| Block non-allowed apps with overlay | `SYSTEM_ALERT_WINDOW`; strings `block_app_overlay_*` ("this app isn't on your Allow List") |
| Bring user back to app | `REORDER_TASKS` |
| Suppress notifications while focusing | `NotificationBlocker` (NotificationListenerService) |
| Distinguish screen-lock from leaving app | `seekrtech.utils.stdevicelockeventmanager` (`STDLEMService`, `JobSchedulerService`) |
| Session lifecycle tracking | `com.seekrtech.devicetracker` Start/End/PauseRestore receivers |
| Survive backgrounding | `FOREGROUND_SERVICE`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |
| Session-end notifications | `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`, `NotificationAlarmReceiver` |

"Deep Focus" mode = allow-list enforcement. Without permissions it is off and leaving the app does not kill the tree
(`close_focus_mode_dialog_android_no_permission`).

## Product rules (observed from strings)

- Two modes: **Timer** (countdown, drag green button to set) and **Stopwatch** (count-up; stopping before 10 min → withered tree)
- Leaving during session: "If you wish to leave Forest, you have to give up on your tree first" → withered tree
- Allow Lists: multiple named lists, a non-editable Default list (system apps), selectable per session
- Coins: earned per session; 2x via rewarded ad, 3x with Plus subscription; spent on species, deleting records
- Deleting a planting record costs coins (free for withered trees if a setting is enabled)
- Group planting: if one member leaves, everyone's tree withers ("%1$s made our tree wither")
- Real trees, Time Crystals (event currency), achievements, ranking, friends, tags, timeline, statistics
- Ambient sounds with descriptions; home-screen widgets (forest, bar chart, favorites)

## Screens (activities)

Splash → Onboarding/Tutorial → Main (plant/growing) · Store · Timeline · NewStatistics · Tag · Ranking/ManageFriends ·
Achievement · RealTree · Settings/AdvancedSettings/AllowList/Premium · Profile/Account · Paywall/Subscription ·
FocusChallenge/StarterChallenges · Shield · Relax · Usage (phone usage)

## Leave detection — `DetectService` (decompiled, static)

`cc.forestapp.activities.main.growing.DetectService` is a BroadcastReceiver holding UsageStatsManager,
ActivityManager and a WindowManager overlay (`CustomOverlayWindowBinding`), with an `OverlaySource` enum.

- `startDetect` (`d0`): sets `isDetectEnabled`, reloads config, then runs a check — ev_735e8a2b…
- `reloadConfigurations` (`L`): reads whether the user's plan allows allow-lists (`UserServiceLevelManager`),
  whether allow-list is on (user default key), resets `curPackageName`/`currentPackageIsNotAllowed`; also loads
  the seasonal skin for the overlay — ev_43ddccbc…
- `onReceive`: on `ACTION_TIME_TICK` (once a minute) re-runs the check (`h0` → `J(callback(timestamp))`) — ev_5d798b34…, ev_b973c6a7…
- `o0(packageName)`: block = `isDetectEnabled && shouldBlock && !allowList.contains(packageName)` — ev_ed4bb409…

Design takeaway for our app: foreground-app polling via UsageStatsManager (plus accessibility events for speed),
compare against allow list, show a TYPE_APPLICATION_OVERLAY window when not allowed; config refreshed per session.

## Data model (decompiled, static; names recovered from `toString()`)

`PlantEntity` — one focus session (Room/SQLite via `ForestDatabase`) — ev_2ef256f7…
- `id`, `serverId` (sync), `startTime`, `endTime`, `endTimeBackup` (tmp end while ongoing)
- `modeString` (`Mode` enum: timer/stopwatch), `treeType` (species int), `tagId`, note
- `isSuccess`, `dieReason`, `roomId` (group "Plant Together" room), `ongoing`
- dirty flag for sync (`getAllDirtyPlants`, `updateServerIdAndDirty`), soft delete, `exceedTime`,
  `extensionTimerId`, CSV export, `trees: List<TreeEntity>`
- `PlantState` enum + inner `TreeState`

`TreeEntity` — individual tree(s) inside a session — ev_dca93254…
- `id`, `plantId`, `treeType`, `dead`, `theme`, `phase` (growth stage)
- Longer sessions can produce several trees (list per plant)

`DieReason` enum — ev_c048defd…
- `GIVE_UP` (user tapped give up / left the app), `KILL_APP` (app was killed during session),
  `OTHERS_GIVE_UP_IN_TOGETHER_MODE` (group member left)

## Coin formula — `DefaultCalculateSuccessFocusEarnedCoinsUseCase.a` (observed) — ev_9c7edcde…

```
m     = floor(focusSecondsIncludingExtension / 60)
base  = (m < 25 ? 1 : 4) + floor(m / 5) + floor(max(m - 30, 0) / 30) * 5
boost: None → base · RewardedAd → base*2 · Plus → base*3 · SunShine → base*3
```
Examples: 10m→3, 25m→9, 30m→10, 60m→21, 90m→32, 120m→43. Only successful sessions earn coins.

## Session lifecycle (stforestkit `app.focus`)

- `FocusRivePhase` enum (animation/UI phases) — ev_26d7ed6d…:
  `Pending → Grace → Focusing → Recap → Result → Summary → Reflect`
- `FocusSessionThresholds` (singleton) — ev_f26f10dd…, ev_83ed0505…:
  `CANCEL_GRACE_WINDOW`; `isWithinGrace(elapsed) = elapsed < CANCEL_GRACE_WINDOW`.
  Built as `PlatformDuration(…, 10)` and a second duration `(…, 1)`. **Inferred** 10 seconds
  (4th arg of a 4-int builder with first 3 defaulted; matches Forest's known "cancel within 10s" behavior).
  Within grace, cancelling ends the session with no withered tree (`cancelIfWithinGrace` → `CancelWithinGraceResult`).
- `FailReason` (new kit) — ev_fa8a47bd…: `GaveUp`, `TogetherParticipantGaveUp`
  (legacy `DieReason` additionally has `KILL_APP`; strings: "Focus Session Canceled… unexpected app closure")
- Stopwatch: stopping before 10 min → withered tree (string `dialog_countup_insifficient_time_content`)
- Give-up confirmation: "Think for %d seconds" delay before the Give Up button activates
- Other domain pieces worth mirroring: `FocusSessionManager` (StateFlow + Mutex), `PersistedFocusSession`
  (serialized ongoing session → survives process death), `FocusTimerIds`/`FocusTimerPair` (alarms),
  `TerminationEvidence`/`TerminationWitness` (records why a session ended), `FocusGiveUpPolicy`

## Screen lock — `STDeviceLockEventManager` — ev_acbbb56a…, ev_05d3272f…

- Records lock/unlock into its own SQLite DB (`STDLEMDatabase`, `DeviceLockEvent`); `c(locked)` writes event
- Schedules a persisted JobScheduler job (id 19991020, `setRequiresDeviceIdle`) to keep tracking
- Purpose (inferred): screen-off/lock periods are excluded from "left the app", so locking the phone does not kill the tree

## Reflect feature (relevant to our LLM idea)

`app.focus.domain.reflect.*`: `ReflectGuidanceComposer`, `ReflectGuidanceInput`, `PlantRecordsFactMemory`,
`ReflectionHistoryMemory`, `FocusSessionFactThresholds`. Rule-based, template messages chosen by
time-of-day (morning/afternoon/evening/night), length (short/long), tag, success/give-up
(strings `reflect_closing_a_fact_*`). Our app can replace this with LLM-generated reflections.

## Open questions

- [x] Coin formula · [x] Grace window (10s, inferred) · [x] Die reasons · [x] Phases · [x] Data model
- [ ] Exact unlock-species prices (server-provided via `CoinRewardDto`/store API — not in APK)
- [ ] Growth-stage timing inside Rive animation (in `.riv` state machine, not code)
- [ ] Sync API endpoints (`com.seekrtech.stforestkit.api.endpoints.*`) — not needed for a personal app
