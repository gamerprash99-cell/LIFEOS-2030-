# 22 — Environment Variables

> **Current project snapshot — 2026-09-11:** This documentation set has been refreshed to match the current LifeOS archive. The latest UI/UX pass covers Timeline, Tasks, Home-first daily math alarm, biometric App Lock, capture controls, landscape video playback, backup export/restore and onboarding restore. The existing Kotlin + Jetpack Compose + Room + manual DI + Compose Navigation architecture and LifeOS color identity are preserved. No Room schema change or destructive database migration was introduced. Android build/device verification remains pending because this coding environment does not provide a usable Android SDK/Gradle toolchain.


## Summary

**This project uses zero build-time environment variables.** Confirmed by
searching the entire repository for System.getenv, custom BuildConfig
fields, local.properties-based secrets, and .env file patterns — none exist.

The only "configuration value" resembling a secret is the AI API key, and
it is deliberately not an environment variable — it's a runtime value the
end user types into the running app (Settings screen), stored via
core/util/SettingsStore.kt (Android DataStore Preferences) on their own device.

## Table (per the requested format)

| Variable | Purpose | Required | Where Used | Example |
|---|---|---|---|---|
| (none exist) | — | — | — | — |

## Current runtime secrets

There is no external AI API key in the current implementation.

| Value | Purpose | Required for | Where entered | Where stored | Example format |
|---|---|---|---|---|---|
| External AI API key | Not used by current implementation | None | None | None | — |

## Standard Android files that could hold secrets (currently empty/absent)

| File | Status in this repo |
|---|---|
| local.properties | Not present in the repo (normal — machine-specific, typically holds the local Android SDK path, not app secrets) |
| .env | Not present, not referenced anywhere in Gradle config |
| keystore.properties (common convention for signing secrets) | Not present — ties to docs/16_KNOWN_ISSUES.md Issue #2 (no signing config exists yet) |

## Recommendation if secrets are ever needed at build time

NOT VERIFIED FROM CODEBASE — forward-looking guidance only. If a future
feature requires a build-time secret (e.g. a shared/default API key, a
signing keystore password), the standard, safe Android pattern is:
1. Add the value to local.properties or a dedicated keystore.properties
2. Read it in app/build.gradle.kts via Properties() loaded from that file
3. Expose it to code only via BuildConfig fields, never hardcoded in a .kt file
4. Ensure the properties file is listed in .gitignore
