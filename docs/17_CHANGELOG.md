# 17 — Changelog

## 2026-09-11 — UI/UX, navigation, backup, alarm and capture pass

### Fixed
- Fixed Home scroll/inset clipping and Home navigation from linked dashboard
  destinations.
- Fixed captured-media metadata layout so date/time remains visible.
- Fixed video fullscreen orientation handling and improved playback controls.

### Added
- Android document-picker based backup export and restore flows.
- Daily alarm with a fresh addition/subtraction challenge for every alarm.
- Morning photo suggestion that saves through the existing Timeline capture
  pipeline.
- Improved shared LifeOS UI components and lightweight animations.

### Security
- Kept PIN App Lock and strengthened biometric App Lock with an
  Android-Keystore-bound credential and biometric-enrollment invalidation.

### Documentation
- README and relevant `/docs` files updated in the same project archive.
- `UPDATE.md` is intentionally not used.



## Important note on how this document was produced

⚠️ **NOT VERIFIED FROM GIT HISTORY** — this repository, as delivered, has no
`.git` directory, so there is no commit log to reconstruct a chronological
history from. The entries below are organized by **development session**
(as the code was authored) rather than by git commits, since that's the
only history that actually exists to draw from. Per the instructions
governing this document, no history has been fabricated beyond what can be
directly inferred from the current state and structure of the code.

Once this repository is pushed to GitHub (`docs/12_GITHUB_WORKFLOW.md`),
all *future* entries in this file should be generated from real `git log`
output.

---

## [0.1.0-phase1-6] — Current state (`app/build.gradle.kts` `versionName`)

### Added — Core data & architecture
- Room database with 7 tables: `notes`, `tasks`, `habits`, `habit_completions`,
  `expenses`, `diary_entries`, `captures` (`data/db/`)
- Repository layer for each feature (`data/repository/`)
- Manual dependency-injection container, `ServiceLocator` (`core/di/`)
- Glassmorphism design system (`ui/theme/`, `ui/components/GlassCard.kt`)

### Added — Features
- Notes with rich-text blocks (paragraph, heading, bullet, numbered, checklist)
- Tasks with priority, due dates, overdue tracking, "keep for tomorrow"
- Habits with real streak calculation and a 12-week GitHub-style heatmap
- Expenses with categories and monthly totals
- Diary with mood tagging
- Unified Timeline aggregating all of the above by date/time (computed live, not stored)
- Global search across Notes/Tasks/Expenses/Diary (plain SQL `LIKE`)
- Home dashboard combining live task/habit/expense data

### Added — AI layer
- Real Anthropic API integration (`core/ai/AiClient.kt`)
- Note AI actions (summarize, rewrite, extract tasks, etc.)
- AI diary drafting with mandatory human-review flow
- AI weekly review summaries
- AI Assistant chat screen

### Added — Capture
- Photo capture via CameraX (`ui/capture/CameraCaptureScreen.kt`)
- Video capture via CameraX `VideoCapture`/`Recorder` (`ui/capture/VideoCaptureScreen.kt`)
- Audio capture via `MediaRecorder` (`ui/capture/AudioCaptureScreen.kt`)
- Text "thought" capture

### Added — Security / access control
- App Lock via biometric/PIN (`core/security/AppLockManager.kt`)
- `android:allowBackup="false"` + backup/data-extraction exclusion rules

### Added — Reminders
- Per-item WorkManager reminder scheduling (`core/reminders/`)
- Notification permission requested only when the user enables Reminders in Settings

### Added — Backup
- Full JSON export of all tables (`data/repository/BackupRepository.kt`)
- Share exported backup via Android's system share sheet (`FileProvider`)

### Added — Onboarding
- 4-page first-launch introduction (`ui/onboarding/OnboardingScreen.kt`)

### Added — Documentation
- Full `/docs` knowledge base (this file and its 24 siblings), created in
  this same working session, grounded in a direct audit of the code above

### Known limitations at this version
See `docs/16_KNOWN_ISSUES.md` for the complete, current list. Headlines:
missing Gradle wrapper scripts, no release signing config, backup restore
has no UI, task recurrence is inert, no automated tests, database/API key
not encrypted at rest.

### Breaking changes
Not applicable — this is the first tracked version.

### Security
See `docs/08_SECURITY.md` for the full classified findings list from this version.

---

## Future entries

Every subsequent version should follow this format:

```
## [x.y.z] — YYYY-MM-DD

### Added
### Changed
### Fixed
### Removed
### Security
### Breaking Changes
```

...and should be generated from real `git log`/PR history, not reconstructed
from reading code after the fact.


## 2026-09-10 — UI/media/security upgrade
- Added landscape fullscreen video viewing with aspect-ratio-safe playback.
- Added shared LifeOS design-system primitives.
- Reorganized Home into the requested dashboard hierarchy.
- Added per-habit current/best streak and weekly/monthly completion metrics to the Habits list.
- Bound biometric App Lock authentication to Android Keystore and biometric enrollment invalidation.
