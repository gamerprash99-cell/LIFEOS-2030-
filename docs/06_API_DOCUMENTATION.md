# API / Integration Documentation

> **Current status — 2026-09-12:** Released LifeOS has **no external HTTP/API integration**. This page is intentionally kept short so old provider documentation cannot be mistaken for the current architecture.

## Local application interfaces

LifeOS UI talks to existing repositories/use cases through the hand-written `ServiceLocator`. Room remains the source of truth. The main UI-facing integration points include:

- `GetHomeSummaryUseCase` — Home data
- `BuildTimelineUseCase` — unified chronological Timeline
- `BackupRepository` — explicit JSON export/import
- `AiRepository` + `LifeOSIntelligenceEngine` — deterministic local intelligence
- `SettingsStore` — local preferences and App Lock configuration
- `AlarmScheduler` — local recurring alarm scheduling

## Network policy

- No `INTERNET` permission.
- No Anthropic/OpenAI/Gemini/OpenRouter endpoint.
- No cloud database.
- No telemetry or analytics service.
- User data remains on-device unless the user explicitly exports/shares it through Android.

Any older provider/API material that may exist in historical notes is superseded by this current implementation. Do not reintroduce a network dependency just to provide an AI-like UI.

## 2026-09-12 — Current-state documentation refresh

Reviewed against the supplied LifeOS source archive during the onboarding/UI and code-cleanup pass. The current first-launch flow is five interactive screens with working Start/Next/Skip/Get started controls and Android JSON restore through the existing local `BackupRepository`. The implementation remains Kotlin + Jetpack Compose + Room + manual ServiceLocator + Compose Navigation, with on-device Intelligence and no mandatory cloud/API dependency. Historical sections are retained where they describe earlier project states.
