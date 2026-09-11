# LifeOS UI/UX Implementation Map

## Current baseline

Date: 2026-09-11

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

### Alarm
Daily alarm scheduling remains local. Each alarm challenge uses a fresh random addition/subtraction problem with a non-negative answer below 99 and the alarm cannot be dismissed through the Android Back button.

### Startup
A short animated LifeOS splash now provides a branded startup transition before onboarding/app-lock/content.

## Architecture boundary

This pass intentionally did not introduce a new navigation framework, database, network service, cloud AI provider, remote database, telemetry system or external UI framework.
