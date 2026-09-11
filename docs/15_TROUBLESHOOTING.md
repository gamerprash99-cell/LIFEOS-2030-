> **CURRENT IMPLEMENTATION — 2026-09-11:** The current app has no external AI API key or HTTP endpoint. Any older provider-key troubleshooting below is historical only and must not be used as a setup requirement.

# 15 — Troubleshooting

> **Current project snapshot — 2026-09-11:** This documentation set has been refreshed to match the current LifeOS archive. The latest UI/UX pass covers Timeline, Tasks, Home-first daily math alarm, biometric App Lock, capture controls, landscape video playback, backup export/restore and onboarding restore. The existing Kotlin + Jetpack Compose + Room + manual DI + Compose Navigation architecture and LifeOS color identity are preserved. No Room schema change or destructive database migration was introduced. Android build/device verification remains pending because this coding environment does not provide a usable Android SDK/Gradle toolchain.


Format: **PROBLEM → CAUSE → SOLUTION**. Only documenting causes that are
directly verifiable from this repository's actual configuration — not
generic Android troubleshooting advice.

---

### PROBLEM: `./gradlew: No such file or directory` when building from the command line

**CAUSE**: The repository ships `gradle/wrapper/gradle-wrapper.properties`
but not the `gradlew` / `gradlew.bat` scripts or `gradle-wrapper.jar` (see
`docs/16_KNOWN_ISSUES.md`, Issue #1).

**SOLUTION**: Open the project in Android Studio first and let it sync —
Android Studio typically regenerates the wrapper automatically. Or, from a
machine with Gradle already installed: `gradle wrapper --gradle-version 8.9`.

---

### PROBLEM: Build fails with a Room/KSP-related error mentioning `schemas`

**CAUSE**: `app/build.gradle.kts` configures
`ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, but no
`app/schemas/` directory exists in the delivered repository — it's only
generated on first successful build.

**SOLUTION**: This should self-resolve on a clean build (Gradle/KSP creates
the directory). If it doesn't, manually create an empty `app/schemas/`
folder and rebuild.

---

### PROBLEM: Release build (`assembleRelease`/`bundleRelease`) fails or produces an unsigned/unusable APK

**CAUSE**: No `signingConfigs` block exists in `app/build.gradle.kts` — this
is confirmed absent, not a guess. See `docs/13_DEPLOYMENT.md`.

**SOLUTION**: A signing configuration must be added (a keystore file +
`signingConfigs { release { ... } }` block referencing it) before a usable
release build can be produced. This is a required setup step, not a bug.

---

### Historical note: external AI/API troubleshooting
The current implementation does not require an AI API key or external AI service. Older provider-specific troubleshooting below is retained only as historical context and should not be used as the current setup path.

**CAUSE**: `core/ai/AiClient.kt`'s `apiKeyProvider` returns null/blank —
either no key has been entered yet, or it wasn't saved.

**SOLUTION**: Open Settings → paste a valid Anthropic API key into the
"Anthropic API key" field → tap "Save API key". Confirm
`SettingsViewModel.saveApiKey()` → `SettingsStore.setAiApiKey()` actually
persists (this can be checked by force-closing and reopening the app; the
field should repopulate via `LaunchedEffect(savedKey)` in `SettingsScreen.kt`).

---

### PROBLEM: AI features fail with `"AI request failed (401): ..."`

**CAUSE**: The API key entered is invalid, expired, or malformed.

**SOLUTION**: Verify the key directly against Anthropic's console/docs;
re-enter it in Settings.

---

### PROBLEM: AI features fail with `"AI request failed (404): ..."` or a model-related error

**CAUSE**: `core/ai/AiClient.kt` hardcodes `model = "claude-sonnet-4-6"`.
This string has not been independently verified against Anthropic's current
model catalog (no network access was available during development).

**SOLUTION**: Check Anthropic's current API documentation for valid model
identifiers and update the `model` value in `AiClient.kt` if needed — it's
a single constructor parameter, changeable in one place.

---

### PROBLEM: Notifications for task/habit reminders never appear (Android 13+)

**CAUSE**: `POST_NOTIFICATIONS` runtime permission was never granted — by
design, `core/util/NotificationHelper.kt` checks this permission and simply
does not post if it's missing (silent no-op, not a crash).

**SOLUTION**: Open Settings → toggle "Reminders" on. This is the only place
in the app that requests `POST_NOTIFICATIONS` (see `SettingsScreen.kt`'s
`RemindersCard`).

---

### PROBLEM: Camera/Video/Audio capture screen shows a permission prompt every time, never remembering the grant

**CAUSE**: If this occurs, the most likely cause is the user (or system)
previously denied the permission with "Don't ask again," which Android
handles by silently keeping `isGranted = false` — `rememberPermissionState()`
in `core/util/PermissionManager.kt` does not distinguish "denied" from
"never asked."

**SOLUTION**: Manually grant Camera/Microphone permission via the device's
system App Info settings for LifeOS. This is standard Android permission
behavior, not an app bug.

---

### PROBLEM: App Lock is enabled but the app opens without prompting

**CAUSE**: `MainActivity.kt`'s `AppLockGate` only triggers biometric
authentication if `activity` successfully casts to `FragmentActivity` — if
`LocalContext.current` doesn't resolve to the actual `FragmentActivity`
instance in some edge case (e.g. certain preview/embedding contexts), the
`LaunchedEffect` guard `if (appLockEnabled && !unlocked && activity != null)`
will silently skip authentication.

**SOLUTION**: This should not occur in normal app launches since
`MainActivity` itself is declared as `FragmentActivity`. If encountered,
verify `MainActivity : FragmentActivity()` in `MainActivity.kt` hasn't been
changed to a different base class — App Lock **depends on this specific
base class** (see `docs/19_DEVELOPER_HANDOVER.md`'s "must not change
casually" list).

---

### PROBLEM: Database changes made in code (e.g. adding a field to an entity) crash the app on launch after upgrading

**CAUSE**: `AppDatabase.kt` has no migration strategy defined and no
`fallbackToDestructiveMigration()` call — Room will throw an
`IllegalStateException` if the schema version isn't bumped with a matching
`Migration` object.

**SOLUTION**: Any entity field change **must** be paired with either (a) a
bumped `version` in the `@Database` annotation plus a `Migration` object, or
(b), for local development only, temporarily uninstalling the app to clear
the old database. **Never use `fallbackToDestructiveMigration()` in a
release build** — it silently deletes all user data on schema mismatch.

---

### PROBLEM: `git` commands fail with "not a git repository"

**CAUSE**: This repository, as delivered, has no `.git` directory — version
control has not been initialized yet.

**SOLUTION**: Run `git init` and follow the steps in `docs/12_GITHUB_WORKFLOW.md`.

---

### PROBLEM: Backup restore fails

**CURRENT STATUS**: Restore is available from Settings and first-run onboarding through Android's document picker.

**CAUSE**: The selected file is not a valid LifeOS JSON backup, or the document provider denied access.

**SOLUTION**: Select a previously exported LifeOS JSON backup and allow the document picker access. Restore uses the existing repository/Room path and does not recreate the database.
