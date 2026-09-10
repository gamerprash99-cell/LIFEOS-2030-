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
│   ├── ai/          Local compatibility layer, AiRepository, AiModels
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
- LifeOS intelligence runs locally on-device using deterministic analysis, lexicons, rules, statistics and templates
- No external AI API, cloud AI endpoint, or API key is required by the app
- Backup/export is a manual, user-triggered action producing a local JSON file
- Camera/mic permissions (declared in the manifest for future capture work)
  are requested at runtime only when that specific feature is used, never
  on first launch

---

## Development Update Log

### 2026-09-11 — LifeOS UI/UX redesign and media/security correction pass

This pass was driven by the supplied device screenshots. The goal was not to replace LifeOS architecture, but to make the existing Compose UI feel intentional, colorful, responsive and easier to use on a real phone.

#### Screen-by-screen changes

**Global design system**
- Restored the LifeOS violet/lavender identity instead of the previous mostly-grey presentation.
- Added stronger Material 3 primary/secondary/container colors, lavender surfaces, clearer borders and consistent rounded cards.
- Expanded `LifeOSDesignSystem.kt` with reusable cards, section headers, icon badges, progress bars and statistic chips.
- Improved bottom navigation selection treatment and kept the existing five-tab navigation architecture.
- Increased shared bottom clearance so the Capture FAB does not sit on top of the last content item.
- Added lightweight Compose animations for progress/completion/content changes without introducing a new UI framework.

**Home**
- Reworked the dashboard into the requested hierarchy: Today → Tasks → Habits → Spending → Recent Activity → Intelligence.
- Replaced oversized empty-looking blocks with compact information cards and progress indicators.
- Made Tasks, Spending, Timeline and Ask LifeOS cards directly actionable.
- Improved Habit previews so the home screen communicates today's progress rather than only showing an emoji/name tile.

**Habits**
- Redesigned each habit row as a richer tracking card.
- Shows today's progress/percentage, current streak, best streak, 7-day completion and 30-day completion directly on the list.
- Added clearer daily progress visualization and larger touch targets.
- Improved the Create Habit flow with selectable icons and a clearer optional reminder action.

**Capture**
- Redesigned the Capture bottom sheet with a clearer hierarchy and larger Photo/Video/Audio actions.
- Added safe-area handling to camera/video controls so buttons stay above gesture/navigation areas.
- Improved capture confirmation so a successful media capture remains visible before the sheet is dismissed.

**Photo detail / Timeline media**
- Fixed the layout problem where the captured photo could push the date/time metadata outside the visible screen.
- Capture detail is now vertically scrollable and uses a dedicated metadata card below the media.
- Date and time are kept readable and no longer depend on a fixed-height screen layout.

**Video**
- Redesigned the video preview with a clear play button, fullscreen action and 16:9 media surface.
- Fullscreen viewer opens in landscape, keeps the source aspect ratio, provides Android playback controls and restores the previous orientation on exit.
- Capture controls were redesigned for clearer recording state and safer bottom positioning.

**Settings / App Lock**
- Fixed the App Lock presentation so the protection method and Change/Set Up action are always visible.
- Restored the actual setup choices: None, Biometric and PIN.
- PIN setup retains 4–6 digit PIN confirmation and mandatory recovery-question/answer flow.
- Biometric setup explicitly verifies the user's strong biometric before enabling the lock.
- Existing Android Keystore-bound biometric hardening remains in place; no biometric data is stored by LifeOS.

#### Data and architecture
- No Kotlin-to-other-language migration.
- No navigation replacement. Existing Compose Navigation remains the navigation system.
- No Room table/entity/schema change was introduced by this UI pass.
- No database reset, recreation or destructive migration was added.
- Existing repositories/ViewModels/service locator remain the data path.
- No external AI service, telemetry, Firebase, cloud database or network API was added.

#### Documentation
- `README.md` is the single development-update log for this archive.
- `UPDATE.md` is intentionally not used.

#### Verification status
- Source-level review and UI implementation were completed against the supplied archive and screenshots.
- This environment does not have a usable Android SDK/Gradle installation and the supplied archive does not contain the Gradle wrapper JAR, so an Android compile or device test is **not** reported as passed.
- First real verification step: open the `LifeOS` folder in Android Studio, allow Gradle sync, then run the debug build on a physical phone/emulator.

#### Remaining verification
- Verify portrait and landscape video playback on-device.
- Verify photo detail date/time placement on small and large phones and with larger font scale.
- Verify PIN and biometric setup/unlock/recovery on a real Android device.
- Run `assembleDebug`, unit tests, instrumentation tests and lint/static analysis in Android Studio/CI.
