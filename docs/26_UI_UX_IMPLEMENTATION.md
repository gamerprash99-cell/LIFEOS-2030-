# LifeOS UI/UX Implementation Map

## Current baseline

Date: 2026-09-12

The UI implementation follows the supplied LifeOS Technical UI/UX Build Prompt while preserving the existing Kotlin, Jetpack Compose, Room, manual ServiceLocator, repositories/use cases and Compose Navigation architecture.

## Shared system

- Purple/violet brand tokens in `ui/theme/Color.kt`
- Typography and spacing tokens in `ui/theme/Type.kt` and `Spacing.kt`
- Rounded shape tokens in `ui/theme/Shape.kt`
- Reusable glass/gradient/card/button/progress/AI components in `ui/components/`
- Safe-area-aware Home and Timeline scrolling
- Intentional motion for splash, progress, cards, AI orb and capture affordances

## Reported issues addressed

### Home scrolling
Sticky section headers keep the current section title readable instead of allowing headings to disappear under the status bar while scrolling.

### Home navigation
The Home bottom-nav action explicitly pops the existing navigation stack back to `home`, fixing the reported failure after opening Spending or Timeline.

### Timeline media
Capture cards show the capture date + time as explicit metadata. Photo/video media uses a stable aspect ratio so metadata is not pushed off-screen or clipped.

### Timeline date handling
`BuildTimelineUseCase` now calculates day boundaries in the device timezone instead of assuming every local day is exactly a UTC epoch-day block.

### Video playback
Fullscreen playback is handled inside the existing activity/dialog flow. It supports landscape orientation, Android Back, play/pause, -10 seconds, +10 seconds, a scrubber, automatic control hiding and playback errors. This avoids the previous separate-activity exit path.

### Backup / restore
Existing Android `CreateDocument` and `OpenDocument` flows remain the source of truth for local export/import. JSON stores LifeOS records and capture metadata; binary media files are not embedded in the JSON backup.

### App Lock
Existing None, Biometric and PIN + recovery flows are preserved. Biometric verification is delegated to Android `BiometricPrompt` and no biometric material is stored by LifeOS.

### Timeline memory cards
The 2026-09-12 pass keeps Timeline as a computed aggregation but changes its presentation to a compact memory trail: time label + dot rail + rounded card, with dedicated type chips for Note, Diary, Task, Habit, Money and Moment. Capture cards continue to open the existing detail flow.

### Multiple math alarms
The Home math-alarm card now supports an arbitrary user-managed list of daily times. DataStore stores the list; `AlarmScheduler` assigns a stable PendingIntent request code per minute-of-day and reschedules each alarm independently. The previous single-alarm keys remain as a compatibility fallback.

### Audio capture
Audio capture now enters the same full-screen Dialog boundary used by Photo/Video. The recording surface respects status/navigation insets and presents a clear ready/recording state without changing the underlying MediaRecorder or capture persistence flow.

### Alarm
Daily math alarm scheduling remains local. Each alarm challenge uses a fresh random addition/subtraction problem with a non-negative answer below 99 and the alarm cannot be dismissed through the Android Back button.

### Startup
A short animated LifeOS splash now provides a branded startup transition before onboarding/app-lock/content.

## Architecture boundary

This pass intentionally did not introduce a new navigation framework, database, network service, cloud AI provider, remote database, telemetry system or external UI framework.

### 2026-09-12 changed files

- `ui/timeline/TimelineScreen.kt` — compact rounded memory-card timeline and complete existing type filters.
- `ui/home/HomeScreen.kt` — multi-alarm add/edit/delete UI while retaining the Home alarm feature.
- `core/util/SettingsStore.kt` — additive DataStore list for daily alarm times with legacy single-alarm fallback.
- `core/reminders/AlarmScheduler.kt` — independent PendingIntent scheduling per daily time.
- `core/reminders/AlarmReceiver.kt` and `BootReceiver.kt` — multi-alarm rescheduling and legacy receiver compatibility.
- `ui/capture/CaptureSheet.kt` and `ui/capture/AudioCaptureScreen.kt` — full-screen Audio Capture and clearer recording UX.
