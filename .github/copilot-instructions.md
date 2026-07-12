---
description: "Global instructions for all agents working in the Eenth workspace"
applyTo: "**"
---

# Eenth — Copilot Instructions

## Project Context
Eenth is an Android app (Kotlin, XML layouts, no Compose). See CLAUDE.md for full agent instructions and ARCHITECTURE.md for system design.

## Rules
- **No Jetpack Compose.** All UI is XML layouts + programmatic view creation.
- Use `SharedPreferences` for all local state — do not introduce Room, DataStore, or SQLite.
- HTTP calls go through `TagRepository.kt` using OkHttp. Do not add Retrofit or other HTTP libraries.
- Dark theme only. Follow existing color scheme in `res/values/colors.xml`.
- Use `dpToPx()` helper in `MainActivity` for programmatic layout measurements.
- Target SDK 36, min SDK 26. Do not use APIs below 26 or above 36 without checking.

## Build
```bash
./gradlew assembleDebug
```

## Patterns to Follow
- Broadcast `ACTION_STATE_CHANGED` after any SharedPreferences change that affects blocking state.
- Add new system packages to the allowlist in `EenthService.kt` if they must never be blocked.
- Group packages stored as `StringSet` under key `group_pkgs_{groupId}` in SharedPreferences.
- Bottom sheets use `BottomSheetDialog` from `com.google.android.material`.
