# 16 — Known Issues

> **Current project snapshot — 2026-09-11:** This documentation set has been refreshed to match the current LifeOS archive. The latest UI/UX pass covers Timeline, Tasks, Home-first daily math alarm, biometric App Lock, capture controls, landscape video playback, backup export/restore and onboarding restore. The existing Kotlin + Jetpack Compose + Room + manual DI + Compose Navigation architecture and LifeOS color identity are preserved. No Room schema change or destructive database migration was introduced. Android build/device verification remains pending because this coding environment does not provide a usable Android SDK/Gradle toolchain.


---

### 2026-09-11 verification note — biometric setup path

The current implementation now uses the platform Android `BiometricPrompt`
flow for setup and unlock, with explicit UI error handling instead of the
previous CryptoObject/Keystore setup path. The platform limitation described
in Issue #13 still applies:
BiometricPrompt authenticates against device-enrolled biometrics. This note
does not change that Android platform behavior.

### Issue #1 — Missing Gradle wrapper scripts

- **Severity**: 🟠 High (blocks command-line builds)
- **Description**: `gradle/wrapper/gradle-wrapper.properties` exists but
  `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` do not.
- **Reproduction**: Run `./gradlew assembleDebug` from a fresh clone.
- **Expected behavior**: Gradle wrapper downloads/builds the project.
- **Actual behavior**: `bash: ./gradlew: No such file or directory`
- **Possible cause**: These files were not generated because the project
  was authored without ever running Gradle (no SDK/network access available
  during development — see `docs/13_DEPLOYMENT.md`).
- **Current workaround**: Open in Android Studio (usually self-heals), or
  run `gradle wrapper --gradle-version 8.9` from a machine with Gradle installed.
  The CI workflow (`.github/workflows/android-build.yml`, added after this
  issue was first logged) works around it by installing Gradle directly via
  `gradle/actions/setup-gradle` and running `gradle assembleDebug` instead
  of `./gradlew assembleDebug` — but this is a workaround, not a fix; local
  command-line builds still need one of the two options above.
- **Status**: Open (CI workaround in place; local `gradlew` still missing)

---

### Issue #2 — No release signing configuration

- **Severity**: 🟠 High (blocks any real release)
- **Description**: `app/build.gradle.kts` has no `signingConfigs` block.
- **Reproduction**: Run `./gradlew assembleRelease`.
- **Expected behavior**: A signed, installable release APK.
- **Actual behavior**: An unsigned APK is produced (or the build may need
  additional configuration depending on Gradle/AGP defaults).
- **Current workaround**: None — this must be set up before any Play
  Store submission or distribution outside development devices.
- **Status**: Open

---

### Issue #3 — [RESOLVED] Backup restore UI was missing

- **Severity**: 🟡 Medium (was)
- **Description**: Restore was implemented in `BackupRepository` but had no user-facing entry point.
- **Fix**: Settings exposes Android's document picker for Restore, and first-run onboarding now also exposes Restore so a fresh installation can recover a previously exported JSON backup.
- **Status**: Resolved.

---

### Issue #4 — Task recurrence (`RepeatRule`) is stored but never acted on

- **Severity**: 🟡 Medium
- **Description**: `TaskEntity.repeatRule` (`NONE/DAILY/WEEKLY/MONTHLY/CUSTOM_DAYS`)
  and `repeatDaysCsv` fields exist and can be set, but no scheduler or job
  ever reads them to auto-create the next occurrence of a recurring task.
- **Reproduction**: Create a task with a repeat rule (not currently exposed
  in any Add Task dialog UI either — see Issue #5) and complete it; no new
  instance is generated for the next day/week/month.
- **Expected behavior**: A completed recurring task should spawn its next occurrence.
- **Actual behavior**: Nothing happens; the field is inert.
- **Current workaround**: Manually re-create the task.
- **Status**: Open

---

### Issue #5 — No UI to set task priority, category, description, or repeat rule

- **Severity**: 🟢 Low
- **Description**: `TaskEntity` supports `priority`, `category`,
  `description`, and `repeatRule`, but `ui/tasks/TasksScreen.kt`'s "Add task"
  dialog only exposes a title field and a reminder-time picker.
- **Reproduction**: Open Tasks → "+" — only a title field and reminder option appear.
- **Expected behavior**: Full task creation matching what the data model supports.
- **Actual behavior**: New tasks are always created with default priority
  (`MEDIUM`), no category, no description.
- **Current workaround**: None via UI.
- **Status**: Open

---

### Issue #6 — Habit completions are not cleaned up when a habit is deleted

- **Severity**: 🟢 Low
- **Description**: `HabitRepository.delete()` deletes the `HabitEntity` row
  only; no `@ForeignKey(onDelete = CASCADE)` exists on `HabitCompletionEntity`,
  and no manual cleanup query is called.
- **Reproduction**: Create a habit, log a few completions, delete the habit.
- **Expected behavior**: Associated `habit_completions` rows are also removed.
- **Actual behavior**: Orphaned rows remain in the `habit_completions` table indefinitely.
- **Current workaround**: None — orphaned data has no functional impact
  today (nothing queries by an unknown `habitId`), but it is a data-hygiene issue.
- **Status**: Open

---

### Issue #7 — AI Assistant chat does not use real app data as context

- **Severity**: 🟢 Low (functional limitation, not a bug)
- **Description**: `AiRepository.chat()` supports an optional `contextBlock`
  parameter, but `ui/ai/AiAssistantScreen.kt` always passes `null`.
- **Reproduction**: Ask the AI Assistant "What tasks do I have today?"
- **Expected behavior** (if this were wired up): A context-aware answer
  referencing real task data.
- **Actual behavior**: The AI has no way to know; it will either say so or
  hallucinate a generic answer.
- **Current workaround**: None — this is a scoped-out feature, not a defect
  in what exists.
- **Status**: Open

---

### Issue #8 — No database encryption at rest

- **Severity**: 🟠 High (security)
- **Description**: Database encryption at rest remains a future hardening item. The current app has no external AI key to protect.
- **Status**: Open — tracked here for visibility alongside functional issues.

---

### Issue #9 — No automated tests exist

- **Severity**: 🟡 Medium (process/quality risk, not a functional bug)
- **Description**: See `docs/14_TESTING.md`.
- **Status**: Open

---

### Issue #10 — [RESOLVED] Capture gave no confirmation after Photo/Video/Audio capture

- **Severity**: 🟠 High (was — core UX bug)
- **Description**: `CaptureSheet.kt` wrote the file and database row correctly, then called `onDismiss()` immediately — the bottom sheet closed with no visible feedback, making it look like capture had silently failed even though the data was saved correctly.
- **Fix**: `CaptureSheet.kt` now shows a CONFIRM state with a real preview of the captured file (`ui/capture/CaptureMediaPreview.kt`) and a "Done" button before closing. A new `CaptureDetailScreen.kt` also lets the user reopen any captured item later from the Timeline (tap-to-open was added to `TimelineScreen.kt` for capture items).
- **Status**: Resolved in the UI/UX polish pass.

---

### Issue #11 — [RESOLVED] Cloud AI dependency removed

- **Severity**: Was 🟠 High
- **Description**: Previously, AI features required a cloud API key with no UI to enter one.
- **Fix**: The entire cloud AI dependency was removed. AI features are now powered by the on-device LifeOS Intelligence Engine (`core/intelligence/`) — no key of any kind is required, ever. See `docs/11_AI_SYSTEM.md` for the full rewrite.
- **Status**: Resolved — this was a bigger fix than closing the gap; the underlying need for a key no longer exists at all.

### Issue #12 — Permanently-denied permissions gave no path forward

- **Severity**: Was 🟠 High (UX — felt broken/stuck)
- **Description**: `core/util/PermissionManager.kt`'s old `rememberPermissionState()` had no way to distinguish "denied, can ask again" from "permanently denied" (Android silently no-ops the system dialog after a second denial). Tapping "Grant permission" after a permanent denial did nothing visible, reading as a repeated/broken request.
- **Fix**: `PermissionManager.kt` now exposes a `PermissionStatus` (GRANTED / NOT_YET_REQUESTED_OR_DENIABLE / PERMANENTLY_DENIED) via `ActivityCompat.shouldShowRequestPermissionRationale()`. All three capture screens now show "Open Settings" instead of a dead-end "Grant permission" button once a permission is permanently denied.
- **Status**: Resolved.

### Issue #13 — Biometric App Lock is device-level, not app-specific (platform limitation, not a bug)

- **Severity**: 🟡 Medium (documented limitation, mitigated with an alternative)
- **Description**: Android does not allow apps to register a separate biometric enrollment from the OS — `BiometricPrompt` always verifies against whatever fingerprint/face/PIN is enrolled at the device level. This means "anyone who can unlock the phone can also pass LifeOS's biometric check" is true by Android platform design, for every app that uses BiometricPrompt (not a LifeOS-specific gap).
- **Mitigation shipped**: A separate **App PIN** option (`AppLockType.PIN`) remains available — independent of the device's own lock screen, with salted-hash storage and secure recovery-question verification.
- **Status**: Open (by Android platform design) with a working App PIN alternative.

## 2026-09-11 — Current verification constraints

- The supplied source archive does not include `gradlew`/`gradlew.bat`, so `assembleDebug`, unit tests, instrumentation tests and lint cannot be truthfully reported as executed from this archive.
- The archive has no `.git` directory, so Git status/recent-commit verification is unavailable here.
- Backup JSON restores database records and capture metadata; binary photo/video/audio files are not embedded in the JSON backup. Media backup should therefore be treated as a separate future product decision rather than silently assumed to be restored.

## 2026-09-12 verification/update note

### Resolved in source
- **Capture half-open/drag UX:** Photo and Video now open as full-screen camera surfaces rather than occupying a partially expanded bottom sheet.
- **Unsupported camera zoom values:** zoom choices are derived from the active camera's real min/max zoom range and pinch-to-zoom is supported.
- **Biometric setup compatibility:** the App Lock adapter now supports strong biometric plus secure device credential fallback, and the app gate re-locks when the app leaves the foreground.
- **Diary discoverability:** Diary remains implemented; Home Quick Actions now exposes it directly.

### Environment limitation
The current archive still lacks the Gradle wrapper scripts and the coding environment has no Gradle executable/Android SDK, so these changes could not be verified by a real APK build or emulator screenshot run in this environment.


### Issue #14 — [RESOLVED] Audio capture opened in a half-height surface

- **Severity**: Was 🟠 Medium (UX)
- **Description**: Audio was rendered inside the capture bottom sheet, so the recording screen appeared only in the lower portion of the device.
- **Fix**: Audio now uses the same full-screen Dialog boundary as Photo/Video and applies status/navigation insets. The existing MediaRecorder and persistence path are unchanged.
- **Status**: Resolved 2026-09-12.

### Issue #15 — [RESOLVED] Daily math alarm supported only one time

- **Severity**: Was 🟠 Medium (feature usability)
- **Description**: The previous Home alarm UI stored one hour/minute pair, preventing schedules such as 07:00 and 20:00 at the same time.
- **Fix**: Added a locally persisted list of daily times with Add/Edit/Delete UI and independent AlarmManager PendingIntents. Existing single-alarm keys remain a migration fallback.
- **Status**: Resolved 2026-09-12.
