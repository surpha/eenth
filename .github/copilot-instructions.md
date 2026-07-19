---
description: "Global instructions for all agents working in the Block workspace"
applyTo: "**"
---

# Block — Copilot Instructions

## Project Context
Block (package: `com.eenth.blocker`) is an Android app blocker (Kotlin, XML layouts, no Compose). See CLAUDE.md for full agent instructions and ARCHITECTURE.md for system design.

## Brand
- App name: **Block**. Branded text: "BLOCK" (unblocked, white) / "BLOCKIN" (blocked, red).
- Tagline: "BLOCK IN". Internal package kept as `com.eenth.blocker` for install continuity.

## Rules
- **No Jetpack Compose.** All UI is XML layouts + programmatic view creation.
- Use `SharedPreferences` for all local state — do not introduce Room, DataStore, or SQLite.
- HTTP calls go through `TagRepository.kt` using OkHttp. Do not add Retrofit or other HTTP libraries.
- Charts use MPAndroidChart v3.1.0 (via JitPack). Do not add other charting libraries.
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
- Focus time archived as `focus_YYYY-MM-DD` (Long, ms) for weekly chart history.
- Both main screens (Home + Insights) must have the mirrored bottom nav bar.
