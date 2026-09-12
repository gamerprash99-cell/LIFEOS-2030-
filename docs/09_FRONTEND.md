# 09 — Frontend

> **Current project snapshot — 2026-09-11:** This documentation set has been refreshed to match the current LifeOS archive. The latest UI/UX pass covers Timeline, Tasks, Home-first daily math alarm, biometric App Lock, capture controls, landscape video playback, backup export/restore and onboarding restore. The existing Kotlin + Jetpack Compose + Room + manual DI + Compose Navigation architecture and LifeOS color identity are preserved. No Room schema change or destructive database migration was introduced. Android build/device verification remains pending because this coding environment does not provide a usable Android SDK/Gradle toolchain.


There is no separate "frontend" project — the Android app itself is the
entire user-facing layer. This document covers the Compose UI structure.

## Application structure

```
app/src/main/java/com/lifeos/app/
├── MainActivity.kt              ← app entry point, sets up theme + gates
├── LifeOSApplication.kt         ← Application subclass, builds ServiceLocator
└── ui/
    ├── theme/                   ← design system (colors, type, shapes)
    ├── components/              ← shared reusable composables
    ├── navigation/               ← Screen routes + NavHost
    ├── home/                    ← Home dashboard
    ├── notes/                   ← Notes list + editor
    ├── tasks/                   ← Tasks list
    ├── habits/                  ← Habits list + detail/heatmap
    ├── expenses/                ← Expenses list + add
    ├── diary/                   ← Diary list + AI draft flow
    ├── timeline/                ← Unified daily timeline
    ├── capture/                 ← Photo/Video/Audio/Thought capture
    ├── insights/                ← Weekly AI review
    ├── search/                  ← Global search
    ├── ai/                      ← AI Assistant chat
    ├── settings/                ← Settings screen
    └── onboarding/              ← First-launch intro
```

## Pages / Routes

Defined in `ui/navigation/Screen.kt` as a sealed class, wired into a single
`NavHost` in `ui/navigation/LifeOSNavHost.kt`:

| Route | Screen | Notes |
|---|---|---|
| `home` | `HomeScreen` | Start destination |
| `notes` | `NotesListScreen` | |
| `notes/editor?noteId={noteId}` | `NoteEditorScreen` | `noteId` optional — absent means "new note" |
| `tasks` | `TasksScreen` | |
| `habits` | `HabitsScreen` | |
| `habits/{habitId}` | `HabitDetailScreen` | |
| `expenses` | `ExpensesScreen` | |
| `diary` | `DiaryScreen` | |
| `timeline` | `TimelineScreen` | |
| `insights` | `InsightsScreen` | |
| `search` | `SearchScreen` | |
| `ai_assistant` | `AiAssistantScreen` | |
| `settings` | `SettingsScreen` | |

Bottom navigation bar (`ui/components/LifeOSBottomBar.kt`) only shows 5 of
these routes: Home, Timeline, Tasks, Habits, Settings (`Screen.bottomNavItems`).
The rest (Notes, Expenses, Diary, Insights, Search) are reached via **quick-link
chips on the Home screen** (`HomeScreen.kt`), not the bottom bar directly —
worth knowing if a new developer expects them in the bottom nav and doesn't
find them there.

## State management

- **Pattern**: One `ViewModel` per screen, exposing `StateFlow`s.
- **No external state library** (no Redux/MVI framework) — this is plain
  Android Architecture Components (`androidx.lifecycle.ViewModel` +
  Kotlin `StateFlow`/`Flow`).
- **ViewModel construction**: via `core/di/LambdaViewModelFactory` (in
  `core/di/LocalServiceLocator.kt`) — a tiny generic factory that lets each
  ViewModel take constructor parameters (repositories) without Hilt.
  Example from `ui/tasks/TasksScreen.kt`:
  ```kotlin
  val viewModel: TasksViewModel = viewModel(
      factory = LambdaViewModelFactory { TasksViewModel(locator.taskRepository) }
  )
  ```

## Dependency access pattern

`core/di/LocalServiceLocator.kt` defines a `CompositionLocal`:
```kotlin
val LocalServiceLocator = staticCompositionLocalOf<ServiceLocator> { error(...) }
```
Provided once in `MainActivity.kt`:
```kotlin
CompositionLocalProvider(LocalServiceLocator provides serviceLocator) { ... }
```
Every screen then does `val locator = LocalServiceLocator.current` to reach
repositories, the AI layer, and settings.

## Media and interaction UX

- `ui/capture/CaptureDetailScreen.kt` keeps media metadata in a dedicated
  section below the media and uses scroll/bottom insets so date/time are not
  clipped on smaller screens.
- `ui/capture/VideoFullscreenViewer.kt` provides landscape fullscreen playback
  with aspect-ratio-safe rendering, a seek bar, and 10-second backward/forward
  controls.
- `ui/capture/MorningPhotoSheet.kt` provides the morning-photo suggestion
  without creating a separate media storage system.
- `ui/settings/AlarmChallengeActivity.kt` provides the full-screen daily alarm
  challenge UI.

## Forms

All forms in this app are simple Compose `AlertDialog`s with
`OutlinedTextField`s (e.g. "Add task" in `TasksScreen.kt`, "Add habit" in
`HabitsScreen.kt`, "Add expense" in `ExpensesScreen.kt`). There is no shared
form-validation library or framework — each screen does its own minimal
validation inline (e.g. `if (title.isBlank()) return`).

## UI system / Design system

`ui/theme/`:
- `Color.kt` — the full LifeOS palette (glassmorphism surfaces, brand
  indigo/violet primary, category accent colors for expenses)
- `Type.kt` — Material 3 `Typography` scale
- `Shape.kt` — large rounded corners (8dp–32dp scale)
- `Theme.kt` — `LifeOSTheme()` composable wiring light/dark `ColorScheme`s,
  plus a custom `LocalGlassColors` CompositionLocal for the glassmorphism effect

`ui/components/`:
- `GlassCard.kt` — the signature translucent card component, used across
  Home, Timeline, Habits, Diary, etc.
- `LifeOSBottomBar.kt` — the 5-item bottom navigation bar
- `ReminderTimePickerDialog.kt` — shared Material 3 `TimePicker` dialog used
  by both the Add Task and Add Habit flows

## Error handling / Loading states

There is no centralized error-handling framework. Each ViewModel handles its
own errors locally and exposes them as a `StateFlow<String?>` that the
screen displays (commonly in an `AlertDialog` or inline `Text`). Examples:
`NoteEditorViewModel.aiResult`, `DiaryViewModel.aiError`,
`SettingsViewModel.exportStatus`.

Loading states are similarly per-screen `StateFlow<Boolean>` (e.g. `aiBusy`
in `NoteEditorViewModel`, `NotesViewModel.kt`), rendered as a
`CircularProgressIndicator` while true.

## API communication

There is no external API communication in the current app. AI screens use
`core/ai/AiRepository`, which delegates to the local
`LifeOSIntelligenceEngine`. Feature screens otherwise reach local data through
the existing repositories and Room.

## Reusable components

- `GlassCard` / `GlassChip` (`ui/components/GlassCard.kt`)
- `LifeOSBottomBar` (`ui/components/LifeOSBottomBar.kt`)
- `ReminderTimePickerDialog` (`ui/components/ReminderTimePickerDialog.kt`)

There is currently no dedicated shared component for buttons, text fields,
or list rows — each screen builds its own `AlertDialog`/`OutlinedTextField`
combinations inline. This is a documented opportunity for future
consolidation (see `docs/18_ROADMAP.md`).


## 2026-09 Design-system upgrade

`ui/components/LifeOSDesignSystem.kt` is the shared Material 3 component layer
for common LifeOS surfaces. It provides reusable section headers, cards, metric
cards, progress indicators, status pills, empty/loading states and completion
badges. Existing navigation, ViewModels, repositories and feature screens remain
the integration boundary; the library does not introduce a new UI framework.

## Current UI baseline — 2026-09-11

The shared UI system now uses purple/violet tokens, 8dp spacing, 20dp screen padding, 16/20/28dp control/card radii, safe-area-aware scrolling, reusable glass/gradient surfaces and intentional motion. Home uses sticky section headers; Timeline media keeps date/time visible; video fullscreen is in-app landscape playback; App Lock and backup remain native Android flows.

## Current UI baseline — 2026-09-12

- Home uses `LazyVerticalGrid(GridCells.Adaptive(minSize = 300.dp))` to adapt between phone and larger-screen layouts while keeping existing callbacks and repository-backed data.
- Home avoids `stickyHeader` section blocks that previously amplified the feeling of excessive whitespace during scrolling.
- Floating actions place the small LifeOS AI assistant action immediately above Capture.
- Photo/video capture is full-screen and edge-to-edge inside a dialog surface; status/navigation insets are applied to controls.
- Camera zoom is clamped to the CameraX-reported range and supports pinch gestures.
- AI chat uses compact bubbles and a persistent bottom composer rather than a large empty conversation surface.
- Diary remains a real Room-backed feature and is exposed from Home Quick Actions.

