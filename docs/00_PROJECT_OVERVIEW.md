# 00 — Project Overview

> **Current project snapshot — 2026-09-11:** This documentation set has been refreshed to match the current LifeOS archive. The latest UI/UX pass covers Timeline, Tasks, Home-first daily math alarm, biometric App Lock, capture controls, landscape video playback, backup export/restore and onboarding restore. The existing Kotlin + Jetpack Compose + Room + manual DI + Compose Navigation architecture and LifeOS color identity are preserved. No Room schema change or destructive database migration was introduced. Android build/device verification remains pending because this coding environment does not provide a usable Android SDK/Gradle toolchain.


*Written for a non-technical founder. No code knowledge required to read this document.*

## What is this application?

**LifeOS** is an Android mobile app (built for phones/tablets running Android) that
acts as a single, private, all-in-one personal life-management tool. It combines:

- **Notes** — a rich-text note-taking app
- **Tasks** — a to-do list with priorities, due dates, and reminders
- **Habits** — a habit tracker with streaks and a GitHub-style "heatmap" calendar
- **Expenses** — a personal spending tracker
- **Diary** — a private journal with mood tags
- **Timeline** — a unified daily feed that automatically pulls together everything
  above (notes written, tasks completed, habits done, money spent, diary entries)
  into one chronological story of the user's day
- **Capture** — a quick way to save a photo, video, voice note, or text thought
  in the moment
- **AI Assistant** *(optional)* — local Intelligence features run on-device; no AI key or cloud connection is required. Historical provider wording elsewhere in this document is superseded by the current implementation.
  note summaries, automatic task extraction from notes, diary drafting help,
  and a weekly review, all opt-in and reviewable before anything is saved

## What problem does it solve?

Most people split this kind of tracking across five or six different apps
(a notes app, a to-do app, a habit tracker, an expense tracker, a diary app).
LifeOS's core idea is that these are really all "the same data" — things that
happened to you, on a particular day, at a particular time — and they should
live in **one connected app** rather than five disconnected ones.

## Target users

⚠️ **NOT VERIFIED FROM CODEBASE** — there is no user-research, persona, or
analytics documentation in this repository. The target audience described
here is inferred from the feature set itself (a personal productivity/journaling
app), not from any product document in the repo.

## Main functionality — current state

Every feature listed above is implemented with real, working code connected
to a real on-device database. See `docs/04_FEATURES.md` for the full,
feature-by-feature breakdown of what's built vs. not built.

## Current development status

**This is a pre-release, single-developer scaffold — not a published or
production-deployed app.** Specifically, as verified directly from the
repository:

| Signal | Status |
|---|---|
| Compiles / has been run on a device or emulator | ⚠️ **NOT VERIFIED** — no build environment was available to compile it during development; see `docs/13_DEPLOYMENT.md` |
| Published to the Google Play Store | ❌ No store listing, signing config, or release keystore exists in the repo |
| Has a backend server | ❌ None — this is a fully local, on-device app (see `docs/02_ARCHITECTURE.md`) |
| Has user accounts / login | ❌ None implemented (see `docs/07_AUTHENTICATION.md`) |
| Has automated tests | ❌ Test dependencies are declared in `app/build.gradle.kts` but no test files exist |
| Has a version control history | ❌ No `.git` directory exists in the delivered repository — version history has not been initialized yet |
| Has CI/CD | ❌ No `.github/workflows` directory exists |

## Technology overview (plain English)

- The app is written in **Kotlin**, Google's modern programming language for Android.
- The screens are built with **Jetpack Compose**, Android's current UI toolkit.
- All data (notes, tasks, habits, expenses, diary entries, captures) is stored
  **only on the user's own phone**, in a local database called **Room** (built
  on SQLite). There is no cloud database and no company server holding user data.
- LifeOS does not require an outside AI service or API key. Intelligence runs locally on-device.

See `docs/20_FOUNDER_GUIDE.md` for plain-English explanations of every one of
these terms.

## Major external services

| Service | Used for | Required? |
|---|---|---|
| External AI API | Not used by the current implementation | No — local Intelligence Engine is used |

No other third-party or cloud service (no Firebase, no analytics SDK, no
crash-reporting SDK, no ads SDK, no payments SDK) is present in the codebase.

## Current limitations

- Cannot be verified to compile/run yet (no SDK/build environment available
  during authoring — see `docs/13_DEPLOYMENT.md`)
- No automated tests
- No CI/CD pipeline
- No cloud backup — backup/export produces a local JSON file only, shared
  manually by the user
- No user accounts — the app is single-user, single-device by design
- Several planned features are not implemented (see `docs/01_PRODUCT_REQUIREMENTS.md`
  and `docs/18_ROADMAP.md`): semantic search, note-to-note linking, calendar
  sync, home-screen widgets, recurring task/habit auto-rollover, encryption
  at rest

## Future direction

⚠️ **NOT VERIFIED FROM CODEBASE** as a formal roadmap document. `docs/18_ROADMAP.md`
lists deferred features that are implied by code comments (e.g. "left as a
Phase 6 hardening item") but there is no separate product roadmap file in
the repository to source this from independently.

## 2026-09-12 — Current-state documentation refresh

Reviewed against the supplied LifeOS source archive during the onboarding/UI and code-cleanup pass. The current first-launch flow is five interactive screens with working Start/Next/Skip/Get started controls and Android JSON restore through the existing local `BackupRepository`. The implementation remains Kotlin + Jetpack Compose + Room + manual ServiceLocator + Compose Navigation, with on-device Intelligence and no mandatory cloud/API dependency. Historical sections are retained where they describe earlier project states.
