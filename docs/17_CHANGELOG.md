# 17 — Changelog

## 2026-09-12 — Responsive UI, capture UX and App Lock reliability

### Changed
- Reworked Home into a compact adaptive dashboard with reduced whitespace and no sticky section headers.
- Added a Home Quick Actions row exposing Capture, Notes, Diary, Tasks and Habits without removing existing navigation.
- Added a compact LifeOS AI floating assistant action directly above Capture.
- Refreshed LifeOS AI chat UI with local status, suggestion chips, compact bubbles and animated conversation scrolling.
- Photo/video capture now opens as a full-screen dialog surface instead of a partially expanded bottom sheet.
- Camera zoom controls now respect each device's actual CameraX zoom range and support pinch-to-zoom.

### Fixed
- Removed the need to drag the capture sheet upward before photo/video controls can be used.
- Prevented unsupported 2×/3× zoom values from being offered on devices whose camera does not expose them.
- App Lock biometric flow now supports secure device-credential fallback and re-locks after the app leaves the foreground.

### Preserved
- Diary writing remains implemented and reachable.
- Existing Room schema, repositories, use cases, ViewModels, navigation, backup/restore, reminders, timeline and capture persistence remain intact.


> **Current project snapshot — 2026-09-11:** This documentation set has been refreshed to match the current LifeOS archive. The latest UI/UX pass covers Timeline, Tasks, Home-first daily math alarm, biometric App Lock, capture controls, landscape video playback, backup export/restore and onboarding restore. The existing Kotlin + Jetpack Compose + Room + manual DI + Compose Navigation architecture and LifeOS color identity are preserved. No Room schema change or destructive database migration was introduced. Android build/device verification remains pending because this coding environment does not provide a usable Android SDK/Gradle toolchain.


## 2026-09-11 — Timeline, Tasks, Home alarm, biometric and capture polish

### Changed
- Rebuilt the Timeline presentation around the supplied reference: compact header, date navigation, five-day strip, filters, memory count, timeline rail, media cards and scroll-in animations. The existing LifeOS color palette is unchanged.
- Reworked Tasks into a cleaner progress-led Material 3 layout with animated completion feedback and a simpler add-task flow. Existing ViewModel/repository behavior remains in place.
- Moved the daily math alarm experience to Home and removed the alarm configuration card from Settings. The Home card keeps the existing local AlarmManager scheduler.
- Changed the alarm challenge to two selectable answers; every wrong choice replaces the problem with a fresh +/− problem.
- Added front/back camera switching and 1×/2×/3× zoom to Photo and Video capture, and made capture mode open at a usable full height so controls do not require dragging the sheet upward.
- Replaced the dialog-based fullscreen video path with a dedicated in-app landscape activity using the same local file, with play/pause, seek bar and ±10-second controls.
- Added a Restore backup entry point to first-run onboarding while keeping the existing Android document-picker export/restore flow.

### Fixed
- Replaced the crash-prone biometric Keystore/CryptoObject setup path with the platform `BiometricPrompt` flow. Setup now launches the real Android biometric UI and only stores the selected App Lock mode after successful verification.
- Fixed bottom-navigation Home behavior by using the same `popUpTo`/`launchSingleTop` navigation strategy for Home as the other root destinations.
- Removed extra nested Timeline top-bar spacing that produced large blank areas and made the title feel hardcoded.

### Architecture / Data
- Kotlin + Jetpack Compose, existing Room entities/DAOs/repositories/use cases, manual DI and Compose Navigation are preserved.
- No Room schema change or destructive migration was introduced.
- No cloud AI, telemetry, Firebase, remote database or mandatory network dependency was introduced.

## 2026-09-11 — Timeline, App Lock setup and Home alarm UX pass

### Changed
- Refined the Timeline screen to match the supplied visual reference:
  date strip, memory filters, timeline rail, richer cards and full-width
  capture previews, without changing the existing LifeOS color palette.
- Added the daily alarm card directly to Home with animated enabled state,
  time control and a Settings details shortcut.
- Changed the alarm challenge from typed input to two-choice answers while
  preserving the existing rule that the alarm keeps sounding until solved.

### Fixed
- Added a native Android biometric-enrollment path when a strong biometric
  is not enrolled, plus a check-again flow before enabling App Lock.
- Hardened the biometric Keystore path so an invalidated local credential is
  replaced safely after biometric enrollment changes.

### Architecture / Data
- No Room schema, repository, use case, navigation architecture or external
  service was introduced or replaced.


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

## 2026-09-11 — Production UI/UX correction pass

### Changed
- Re-established purple/violet visual identity in shared theme tokens for light and dark modes.
- Added reusable glass/gradient UI primitives, progress ring, AI orb, offline pill and standardized button/card treatment.
- Redesigned Home hierarchy and added sticky section headers plus safe-area-aware scrolling.
- Fixed bottom navigation Home routing from nested Spending/Timeline screens.
- Improved Timeline capture metadata so date + time remain visible and corrected local-time day boundaries.
- Reworked video fullscreen to stay in-app with landscape playback, Back handling, seek backward/forward, scrubber and error state.
- Added animated startup splash and Appearance → Dark LifeOS setting.

### Verification
- No Room schema change.
- No architecture replacement.
- No external AI/network dependency added.
- Gradle verification unavailable because the supplied archive has no Gradle wrapper scripts.
