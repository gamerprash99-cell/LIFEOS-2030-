# 04 — Features

> **Current project snapshot — 2026-09-11:** This documentation set has been refreshed to match the current LifeOS archive. The latest UI/UX pass covers Timeline, Tasks, Home-first daily math alarm, biometric App Lock, capture controls, landscape video playback, backup export/restore and onboarding restore. The existing Kotlin + Jetpack Compose + Room + manual DI + Compose Navigation architecture and LifeOS color identity are preserved. No Room schema change or destructive database migration was introduced. Android build/device verification remains pending because this coding environment does not provide a usable Android SDK/Gradle toolchain.


Full inventory of every implemented feature, with exact file paths.

---

## 1. Notes

**Purpose**: Rich-text note-taking with organization (pin/favorite/archive/trash) and optional AI actions.

- **User flow**: Notes list (`ui/notes/NotesListScreen.kt`) → tap note or "+" → `ui/notes/NoteEditorScreen.kt` → edit title/blocks → back (auto-saves).
- **Files**: `ui/notes/NotesListScreen.kt`, `ui/notes/NoteEditorScreen.kt`, `ui/notes/NotesViewModel.kt` (contains both `NotesListViewModel` and `NoteEditorViewModel`)
- **Domain model**: `domain/model/NoteBlock.kt` — a sealed class (`Paragraph`, `Heading`, `BulletItem`, `NumberedItem`, `ChecklistItem`), serialized to JSON and stored in `NoteEntity.contentJson`
- **Database**: `data/db/entities/NoteEntity.kt`, `data/db/dao/NoteDao.kt`
- **Repository**: `data/repository/NoteRepository.kt`
- **AI integration**: `core/ai/AiModels.kt` (`NoteAiAction` enum: Summarize, Generate title, Organize text, Rewrite, Improve grammar, Make shorter/longer, Extract important points, Create checklist, Generate ideas, Explain content, Create study questions) plus "Extract tasks" — all routed through `AiRepository.runNoteAction()` / `extractTasks()`
- **Auth requirement**: None
- **Error handling**: Local Intelligence results remain reviewable before persistent writes; no external AI API key is required by the current implementation
- **Status**: Implemented
- **Known limitations**: No note-to-note linking; search is plain SQL LIKE, not semantic; extracted tasks can only be approved if the note has already been saved once (noteId must be non-null — see `NoteEditorViewModel.approveExtractedTasks`)

## 2. Tasks

**Purpose**: To-do list with priority, due date, reminders, and "keep for tomorrow" rescheduling.

- **User flow**: `ui/tasks/TasksScreen.kt` → "+" → enter title, optionally set a reminder time via `ui/components/ReminderTimePickerDialog.kt` → Add
- **Files**: `ui/tasks/TasksScreen.kt` (contains `TasksViewModel`)
- **Database**: `data/db/entities/TaskEntity.kt`, `data/db/dao/TaskDao.kt`
- **Repository**: `data/repository/TaskRepository.kt`
- **Reminders**: `core/reminders/ReminderScheduler.kt` schedules a WorkManager job when a reminder time is set; cancelled automatically on completion or deletion
- **Status**: Implemented
- **Known limitations**: `RepeatRule` field exists (NONE/DAILY/WEEKLY/MONTHLY/CUSTOM_DAYS) but nothing auto-creates the next recurrence — see `docs/16_KNOWN_ISSUES.md`

## 3. Habits

**Purpose**: Habit tracking with streaks, a GitHub-style heatmap, and goal-count habits (e.g. "drink 8 glasses of water").

- **User flow**: `ui/habits/HabitsScreen.kt` (list + add, with optional reminder) → tap "Details" → `ui/habits/HabitDetailScreen.kt` (streak stats + 12-week heatmap + "Log today's progress" button)
- **Files**: `ui/habits/HabitsScreen.kt`, `ui/habits/HabitDetailScreen.kt`
- **Database**: `data/db/entities/HabitEntity.kt` (habit definition), `HabitCompletionEntity` (one row per habit per day)
- **Repository**: `data/repository/HabitRepository.kt` — contains the actual streak/heatmap math (`computeAnalytics()`, `computeHeatmap()`), computed live from `habit_completions` rows, not hardcoded
- **Domain models**: `domain/model/Categories.kt` (`HabitAnalytics`, `HeatmapCell`, `HeatmapIntensity`)
- **Status**: Implemented

## 4. Expenses

**Purpose**: Personal expense logging with categories and monthly totals.

- **User flow**: `ui/expenses/ExpensesScreen.kt` → "+" → amount, category (from `domain/model/Categories.kt`'s `ExpenseCategories.ALL`), optional merchant → Save
- **Database**: `data/db/entities/ExpenseEntity.kt`, `data/db/dao/ExpenseDao.kt` (includes `getCategoryTotals` for category breakdowns)
- **Repository**: `data/repository/ExpenseRepository.kt`
- **Status**: Implemented
- **Known limitations**: No editing of an existing expense (only add + implicit list); no budget/limit feature

## 5. Diary

**Purpose**: Private journal with mood tagging and an optional AI-drafting assist.

- **User flow**: `ui/diary/DiaryScreen.kt` → "+" → write freely, pick a mood, OR tap "Turn into a diary entry with AI" to have the AI turn rough notes into a polished entry (saved as an unreviewed AI draft)
- **Database**: `data/db/entities/DiaryEntity.kt` — `aiGenerated` and `isReviewed` fields implement the "AI drafts must be approved" rule
- **Repository**: `data/repository/DiaryRepository.kt` (`approveAiDraft()`)
- **AI**: `AiRepository.draftDiaryEntry()`
- **Status**: Implemented — AI drafts show an "AI draft — needs review" label with an Approve button until confirmed

## 6. Timeline

**Purpose**: A single, day-by-day feed merging Notes, completed Tasks, completed Habits, Expenses, Diary entries, and Captures, sorted by time.

- **Files**: `ui/timeline/TimelineScreen.kt` (date navigation with previous/next arrows), `domain/usecase/BuildTimelineUseCase.kt` (the aggregation logic)
- **How it works**: Not a database table — `BuildTimelineUseCase` queries all six repositories for a given day and merges the results into `domain/model/TimelineItem.kt` objects, sorted by `timeMinutes`.
- **Status**: Implemented

## 7. Global Search

**Purpose**: Search across Notes, Tasks, Expenses, and Diary in one screen.

- **Files**: `ui/search/SearchScreen.kt`
- **How it works**: Calls `search(query)` on `NoteRepository`, `TaskRepository`, `ExpenseRepository`, `DiaryRepository` — each of which runs a plain SQL LIKE '%query%' query (see e.g. `NoteDao.search()`). This is not full-text search (no SQLite FTS extension) and not semantic/AI search.
- **Status**: Implemented (basic substring search only)

## 8. Home Dashboard

**Purpose**: At-a-glance view of today's tasks, habits, and spending.

- **Files**: `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`, `domain/usecase/GetHomeSummaryUseCase.kt`
- **How it works**: `GetHomeSummaryUseCase` combines five live Flows (today's tasks, overdue tasks, all habits, today's habit completions, today's expense total) with Kotlin's combine() operator into one `HomeSummary` object.
- **Status**: Implemented

## 9. Capture (Photo / Video / Audio / Thought)

**Purpose**: Quick, in-the-moment capture of a memory.

- **Files**: `ui/capture/CaptureSheet.kt` (capture menu; Photo/Video open full-screen camera surfaces), `ui/capture/CameraCaptureScreen.kt` (photo, via CameraX ImageCapture), `ui/capture/VideoCaptureScreen.kt` (video, via CameraX VideoCapture/Recorder), `ui/capture/AudioCaptureScreen.kt` (via android.media.MediaRecorder), `ui/capture/CaptureDetailScreen.kt` (full viewer, added in the UI/UX pass), `ui/capture/CaptureMediaPreview.kt` + `MediaPreviewUtils.kt` (shared preview composables, added in the UI/UX pass)
- **Storage**: `core/util/MediaStorage.kt` — all captured files are written to app-private storage (context.filesDir/captures/), never to shared/public storage or MediaStore
- **Permissions**: Requested at the moment the relevant capture screen opens, via `core/util/PermissionManager.kt`'s `rememberPermissionState()` — never at app launch
- **Database**: `data/db/entities/CaptureEntity.kt`, `data/repository/CaptureRepository.kt` (now includes `getById()`, a minimal additive read method added for the Detail screen — no schema change)
- **Post-capture confirmation**: after a Photo/Video/Audio capture, `CaptureSheet` shows a CONFIRM state with a real preview of the captured file before closing, instead of dismissing silently.
- **Timeline integration**: tapping a capture item in `TimelineScreen.kt` now opens `CaptureDetailScreen`, showing the real preview, date/time, and a Delete action.
- **Status**: Implemented (all four capture types are real, working code, not stubs; post-capture confirmation and a Detail/viewer screen were added)

## 10. AI Assistant (Chat)

**Purpose**: Free-form chat with the AI about the user's day/data.

- **Files**: `ui/ai/AiAssistantScreen.kt`
- **How it works**: Sends the running conversation to `AiRepository.chat()` through the local `LifeOSIntelligenceEngine`. The current UI is an offline chat surface; it does not require a cloud provider or API key.
- **Status**: Implemented, with the context-injection limitation noted above

## 11. AI Insights / Weekly Review

**Purpose**: AI-generated summary of the week's tasks/spending/diary activity.

- **Files**: `ui/insights/InsightsScreen.kt`
- **How it works**: Computes real stats (tasks completed, total spend, diary entry count) for the current week directly from the repositories, then passes that stats block to `AiRepository.generateReviewSummary()`.
- **Status**: Implemented

## 12. App Lock

**Purpose**: Biometric/PIN gate on the whole app.

- **Files**: `core/security/AppLockManager.kt`, gated in `MainActivity.kt`'s `AppLockGate` composable
- **How it works**: Uses `androidx.biometric.BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` — accepts a strong device biometric or secure device PIN/pattern/password as the platform fallback, never a LifeOS-specific password.
- **Toggle**: `ui/settings/SettingsScreen.kt`, persisted via `core/util/SettingsStore.kt`
- **Status**: Implemented

## 13. Reminders / Notifications

**Purpose**: Task and Habit reminders delivered as Android notifications.

- **Files**: `core/reminders/ReminderScheduler.kt`, `core/reminders/ReminderWorker.kt`, `core/util/NotificationHelper.kt`
- **How it works**: One OneTimeWorkRequest per reminder (not a recurring poll), uniquely named per task/habit so re-setting a reminder replaces the old job. ReminderWorker re-checks the item is still open before notifying.
- **Permission**: POST_NOTIFICATIONS (Android 13+) requested only when the user turns on the "Reminders" toggle in Settings (`SettingsScreen.kt`'s `RemindersCard`)
- **Status**: Implemented

## 14. Backup / Export / Share

**Purpose**: Full data export to a local JSON file, shareable via Android's share sheet.

- **Files**: `data/repository/BackupRepository.kt`, wired into `ui/settings/SettingsScreen.kt`
- **How it works**: `buildBackup()` collects every table into one LifeOSBackup object, serialized to pretty-printed JSON via kotlinx.serialization, written to context.filesDir (app-private storage). "Share" uses androidx.core.content.FileProvider (declared in AndroidManifest.xml + res/xml/file_paths.xml) to hand the file to any share target.
- **Import**: `BackupRepository.importFromFile()` exists and is fully implemented, but ⚠️ no Settings UI button currently calls it — restore is code-complete but not user-reachable yet.
- **Status**: Partially implemented (export: complete and reachable; import: complete but not wired to any UI)

## 15. Onboarding

**Purpose**: 4-page first-launch introduction.

- **Files**: `ui/onboarding/OnboardingScreen.kt`, gated in `MainActivity.kt`'s `OnboardingGate`
- **Status**: Implemented — shown once, persisted via `SettingsStore.onboardingComplete`

## 16. Settings

**Purpose**: App Lock toggle, AI feature toggle, Reminders toggle, Backup/Export/Share.

**Current update:** the current AI/Intelligence implementation is local and does not require an Anthropic API key. Older provider-key notes in this historical section are superseded by `core/intelligence/*` and the current `docs/11_AI_SYSTEM.md`.

- **Files**: `ui/settings/SettingsScreen.kt`
- **Status**: Implemented


## 10. Daily Alarm

**Purpose**: A local daily alarm that requires solving a fresh arithmetic
challenge before the alarm can be dismissed.

- **Files**: `core/reminders/AlarmScheduler.kt`,
  `core/reminders/AlarmReceiver.kt`,
  `ui/settings/AlarmChallengeActivity.kt`,
  `ui/settings/SettingsScreen.kt`
- **Scheduling**: Android `AlarmManager`, with daily rescheduling after the
  alarm fires.
- **Challenge**: A fresh addition or subtraction problem is generated for
  each alarm instance. Operands and the answer are kept within the small
  requested range, and the answer field is limited to two digits.
- **Dismissal**: The alarm sound loops until the correct answer is entered;
  Back cannot dismiss the active challenge.
- **Network**: None.

## 11. Morning Photo

**Purpose**: Encourage a morning face/moment photo and place it directly into
the existing LifeOS memory timeline.

- **UI**: `ui/capture/MorningPhotoSheet.kt` and the Home integration in
  `ui/home/HomeScreen.kt`.
- **Storage**: Uses the existing CameraX capture flow and persists a normal
  `CaptureEntity`, so the photo appears through the existing Timeline
  aggregation.
- **Permissions**: Camera permission is requested only when the user chooses
  the morning photo action.
