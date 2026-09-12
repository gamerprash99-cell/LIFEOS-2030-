# CI Build Fix — 2026-09-12

## Scope

Targeted fixes for the failing `Android CI Build` Kotlin compilation on `main`.

## Changes made

- Enabled the Kotlin serialization compiler plugin required by `@Serializable` backup models and `serializer()` calls.
- Restored timeline repository range-query APIs used by `BuildTimelineUseCase`.
- Corrected Compose `AndroidView` modifier/update argument usage in capture previews and fullscreen video.
- Corrected capture repository calls to use named parameters.
- Fixed App Lock setup callback scope and Compose modifier argument ordering.
- Fixed App Lock row-weight helper usage.
- Fixed Expenses row-weight helper and progress indicator call.
- Fixed Notes and Insights Compose `padding` imports.
- Fixed onboarding `AnimatedContent` transition to use `ContentTransform`.
- Fixed bottom-navigation window-inset usage.
- Fixed AI assistant missing Compose foundation/card imports.

## Verification

GitHub Actions verification is pending after these changes are pushed to `main`.
