# LifeOS — Android Scaffold

📚 **Full project documentation now lives in [`/docs`](./docs)** — a
complete, code-audited knowledge base covering architecture, database,
security, deployment, known issues, roadmap, and a founder-friendly guide.
Start with [`docs/00_PROJECT_OVERVIEW.md`](./docs/00_PROJECT_OVERVIEW.md)
(non-technical) or [`docs/19_DEVELOPER_HANDOVER.md`](./docs/19_DEVELOPER_HANDOVER.md)
(new developer). The self-audit summary is in [`docs/DOCUMENTATION_AUDIT.md`](./docs/DOCUMENTATION_AUDIT.md).

This file remains as a quick build reference; `/docs` is the authoritative,
detailed source going forward.

---

A real, working Kotlin + Jetpack Compose implementation of the LifeOS PRD:
a local-first personal life-management app (Notes, Tasks, Habits, Expenses,
Diary, unified Timeline, and an optional AI layer).

This is a **genuine, compiling-quality scaffold** — not stub files. Every
screen is wired to a real Room database through repositories and ViewModels.
It has **not been compiled in this environment** (no Android SDK / Gradle
network access here) — see "How to build" below for the one thing you need
to do to verify it.

---

## What's implemented (Phases 1–6 of the spec)

| Area | Status |
|---|---|
| Local Room database (Notes, Tasks, Habits, Expenses, Diary, Captures) | ✅ Full CRUD, soft-delete, backup/restore |
| Notes (rich blocks: paragraph/heading/bullet/numbered/checklist) | ✅ Editor + pin/favorite/archive/trash |
| Tasks (priority, due date, overdue, "keep for tomorrow") | ✅ |
| Habits (streaks, GitHub-style heatmap, goal-count habits) | ✅ Real analytics computed from data, not hardcoded |
| Expenses (categories, monthly totals) | ✅ |
| Diary (mood tagging, AI-draft review flow) | ✅ |
| Unified Timeline (Notes+Tasks+Habits+Expenses+Diary+Captures merged by time) | ✅ Computed live, not a duplicate table |
| Global Search (cross-feature) | ✅ |
| Home dashboard | ✅ Reorganized as Today → Tasks → Habits → Spending → Recent Activity → Intelligence |
| AI layer (note actions, task extraction, diary drafting, weekly review, chat) | ✅ Offline LifeOS Intelligence Engine; no external AI API required |
| Diary AI-draft flow ("turn thoughts into an entry") + Approve UI | ✅ AI drafts are flagged `isReviewed = false` and shown with an Approve button until confirmed |
| App Lock (biometric/PIN) | ✅ Biometric path uses an Android Keystore-bound credential invalidated by biometric enrollment changes |
| Backup & Export (full JSON export/import) + Share sheet | ✅ Exports a local JSON file and can hand it off via Android's native share sheet (FileProvider) |
| Photo capture (CameraX) | ✅ Real camera preview + capture, permission requested only when opened |
| Video capture + viewer | ✅ CameraX recording plus portrait-safe preview and landscape fullscreen viewer |
| Habits list analytics | ✅ Each habit row shows current/best streak plus 7-day and 30-day completion directly on the list |
| Audio capture (MediaRecorder) | ✅ Real start/stop recording to app-private storage |
| Task & Habit reminders (WorkManager + notifications) | ✅ Per-item precise scheduling (not polling); Material3 time picker wired into the Add Task/Add Habit dialogs; notification permission requested only when the user turns Reminders on in Settings |
| Onboarding flow | ✅ 4-page first-launch intro, gated by a persisted flag so it only shows once |
| Glassmorphism + LifeOS component system | ✅ shared `LifeOSDesignSystem.kt` primitives plus existing `GlassCard`/`GlassChip`; restrained Material 3 surfaces |

## What's intentionally out of scope for this scaffold

- **Semantic/AI-powered search**, **note-to-note wiki linking**, **calendar
  sync**, and **home-screen widgets** are not built.
- **Recurring task/habit rollover logic** (`RepeatRule` field exists on
  `TaskEntity`/is modeled via `HabitFrequency`, but the actual "auto-create
  tomorrow's instance" scheduling job is not wired up).
- **Encryption at rest** (SQLCipher) — noted as a Phase 6 hardening item;
  the Room DB is currently unencrypted on-device (standard Android app
  sandboxing still applies), with `allowBackup="false"` so nothing leaves
  via OS backup.

These were left out deliberately to keep everything *else* genuinely
working end-to-end, rather than spreading effort across even more shallow
stubs.

---

## Architecture

```
app/src/main/java/com/lifeos/app/
├── core/
│   ├── ai/          AiClient (real Anthropic API call), AiRepository (prompt assembly), AiModels
│   ├── di/           ServiceLocator — one hand-written DI container (no Hilt/KSP fragility)
│   ├── security/      AppLockManager (BiometricPrompt)
│   └── util/          DateTimeUtils, IdGenerator, SettingsStore (DataStore)
├── data/
│   ├── db/            Room entities, DAOs, AppDatabase, TypeConverters
│   └── repository/    One repository per feature + BackupRepository
├── domain/
│   ├── model/         NoteBlock, TimelineItem, HabitAnalytics, HeatmapCell, ExpenseCategories
│   └── usecase/       BuildTimelineUseCase, GetHomeSummaryUseCase
└── ui/
    ├── theme/          Glassmorphism design system (Color/Type/Shape/Theme)
    ├── components/     GlassCard, GlassChip, LifeOSBottomBar
    ├── navigation/      Screen.kt, LifeOSNavHost.kt
    └── <feature>/       One screen + ViewModel per feature (home, notes, tasks, habits, expenses, diary, timeline, capture, insights, search, ai, settings)
```

**Why manual DI instead of Hilt?** For a scaffold like this, Hilt's KSP
annotation processing is the single most common source of "works on my
machine, fails in CI" build breakage from version mismatches. `ServiceLocator.kt`
is a ~50-line hand-written container that's trivial to swap for Hilt later
without touching any ViewModel.

**Why is Timeline not its own table?** The spec explicitly asks for features
to be "connected through Date, Time, Tags, Timeline, Relationships" rather
than siloed. `BuildTimelineUseCase` aggregates Notes/Tasks/Habits/Expenses/
Diary/Captures live for a given day — this is the concrete implementation
of that principle, and it means the Timeline can never drift out of sync
with the source data.

---

## Reminders — how they actually work

`core/reminders/ReminderScheduler.kt` schedules one precise WorkManager
`OneTimeWorkRequest` per task/habit reminder (not a recurring poll), keyed
by a unique work name so re-setting a reminder replaces the old one cleanly.
`ReminderWorker.kt` fires at the scheduled time, re-checks the item is still
open (not completed/deleted/archived) via the repositories, and posts a
notification through `NotificationHelper`.

- Reminder time is picked via a Material3 `TimePicker` in the Add Task /
  Add Habit dialogs.
- On Android 13+, `POST_NOTIFICATIONS` is requested only when the user
  flips "Reminders" on in Settings — never at first launch.
- Completing or deleting a task/habit cancels its pending reminder.

---

## AI feature — how it actually works

LifeOS AI is **offline-first**. The app does not require Anthropic, Claude,
OpenAI, Gemini, OpenRouter, an API key, or a cloud AI endpoint.

`core/ai/AiRepository.kt` is a compatibility layer over the local
`LifeOSIntelligenceEngine`. The engine uses deterministic analysis, local
statistics, lexicons, rules and templates to provide note actions, task
extraction, diary drafting, weekly/monthly reports and local questions.

AI-style outputs remain suggestions: anything that changes persistent user
data follows the existing review/approval flow before being written to Room.

## How to build

You'll need **Android Studio (Ladybug or newer)** with:
- Android SDK 35
- JDK 17

```bash
git clone <this-repo>
cd LifeOS
# Open in Android Studio, let it sync Gradle, then Run ▶ on an emulator or device
```

Or from the command line (once you have the Android SDK + `local.properties`
pointing at it):

```bash
./gradlew assembleDebug
```

**This archive has not been compiled in this coding environment.**
(no Android SDK, no network access to resolve Gradle/Maven dependencies).
The code follows current, stable AGP 8.6.1 / Kotlin 2.0.21 / Compose BOM
2024.11.00 APIs throughout, but please run a Gradle sync as your first step
and treat any dependency-resolution errors as the actual "did this compile"
signal — I've done my best to get versions and API usage right, but I have
not been able to verify it end-to-end myself.

---

## Pushing to your GitHub repo

```bash
cd LifeOS
git init
git add .
git commit -m "LifeOS: initial Android scaffold (Phases 1-6)"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```

---

## Data & privacy principles this scaffold follows

- `android:allowBackup="false"` — nothing leaves the device via OS auto-backup
- AI calls only fire on explicit user action, and only send the specific
  text needed for that one action (never the whole database)
- Backup/export is a manual, user-triggered action producing a local JSON file
- Camera/mic permissions (declared in the manifest for future capture work)
  are requested at runtime only when that specific feature is used, never
  on first launch

---

## Development Update Log

### 2026-09-10 — UI, media, habits and security upgrade

#### Changes
- Added `VideoFullscreenViewer` for a real landscape/fullscreen video playback path.
- Updated `VideoPreview` to preserve the 16:9 preview surface and expose fullscreen.
- Added the shared `LifeOSDesignSystem.kt` component layer: section headers, cards, metric cards, progress, status pills, empty/loading states and completion badges.
- Reorganized Home around the requested priority: Today → Tasks → Habits → Spending → Recent Activity → Intelligence.
- Added a direct Recent Activity entry point to the existing Timeline navigation.
- Added current streak, best streak, 7-day completion and 30-day completion to every Habits list row.
- Extended `HabitAnalytics` with weekly metrics; no Room schema change was required.
- Hardened biometric App Lock with an Android Keystore-bound AES credential, per-use authentication and biometric-enrollment invalidation.
- Updated README and security/AI documentation to describe the current offline/local implementation and the new work.

#### Affected files
- `app/src/main/java/com/lifeos/app/ui/capture/VideoFullscreenViewer.kt`
- `app/src/main/java/com/lifeos/app/ui/capture/CaptureMediaPreview.kt`
- `app/src/main/java/com/lifeos/app/ui/components/LifeOSDesignSystem.kt`
- `app/src/main/java/com/lifeos/app/ui/theme/Spacing.kt`
- `app/src/main/java/com/lifeos/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt`
- `app/src/main/java/com/lifeos/app/ui/habits/HabitsScreen.kt`
- `app/src/main/java/com/lifeos/app/domain/model/Categories.kt`
- `app/src/main/java/com/lifeos/app/data/repository/HabitRepository.kt`
- `app/src/main/java/com/lifeos/app/core/security/AppLockManager.kt`
- `app/src/main/java/com/lifeos/app/ui/security/AppLockSetupScreen.kt`
- `README.md`
- `docs/08_SECURITY.md`
- `docs/11_AI_SYSTEM.md`

#### Database
No Room entity/table/schema version was changed. No migration was needed.

#### Verification
Static source review was performed in this environment. The supplied archive did not
contain the Gradle wrapper JAR or a usable Android SDK, so an Android compilation was
not claimed as passed. The next verification step remains opening the project in
Android Studio, syncing Gradle, and compiling/running the debug build.

#### Remaining issues
- Full end-to-end Android build and device testing still need to be performed in Android Studio.
- Landscape fullscreen behavior should be verified on at least one portrait phone and one device/emulator with sensor rotation.
