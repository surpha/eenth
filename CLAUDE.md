# CLAUDE.md — Agent Instructions for Eenth

## What is Eenth?
Eenth is an open-source, physical-first Android app blocker. Users pair an NFC tag ("brick") to their phone. Tapping the tag bricks/unbricks their device — blocking all selected apps with a full-screen overlay. No willpower needed; you must physically find your brick to get your apps back.

## Tech Stack
- **Language:** Kotlin (JVM target 11)
- **UI:** Standard Android XML Layouts (NOT Compose) with AppCompat + Material Design
- **Storage:** SharedPreferences (`eenth_prefs`)
- **Blocking engine:** AccessibilityService (monitors `TYPE_WINDOW_STATE_CHANGED`)
- **NFC:** `NfcAdapter.ReaderCallback` for tag read
- **Server:** Supabase REST API (tag registration/verification)
- **HTTP client:** OkHttp 4.12.0
- **Build:** Gradle with version catalog (`gradle/libs.versions.toml`)
- **Min SDK:** 26, Target/Compile SDK: 36

## Build & Deploy
```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
$HOME/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# If multiple devices, specify serial:
$HOME/Library/Android/sdk/platform-tools/adb -s <SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure
```
app/src/main/java/com/eenth/blocker/
├── MainActivity.kt          # Main UI — status card, tag section, groups, app list
├── EenthService.kt          # AccessibilityService — detects foreground app, launches BlockerActivity
├── BlockerActivity.kt       # Full-screen blocker overlay + NFC reader for direct unbrick
├── NfcUnlockActivity.kt     # Handles system NFC intent dispatch (TAG_DISCOVERED)
├── TagRepository.kt         # Supabase REST client for tag pair/verify/unpair/updateName
├── AppGroup.kt              # AppGroup data class + GroupManager (presets + custom groups)
├── GroupAdapter.kt           # RecyclerView adapter for group cards (overlapping app icons)
├── AppListAdapter.kt         # RecyclerView adapter for the app list (icon + name + switch)
└── ui/theme/                 # Color, Theme, Type (unused Compose leftovers)
```

## Key Concepts

### Blocking Flow
1. `EenthService` (AccessibilityService) observes every window change
2. Checks if app is in: `blocked_apps` OR any selected group's packages OR `brick_everything` is on
3. If blocked + bricked → launches `BlockerActivity` (full-screen wall)
4. User must tap their NFC brick on `BlockerActivity` to unbrick

### SharedPreferences Keys (`eenth_prefs`)
| Key | Type | Purpose |
|-----|------|---------|
| `blocked_apps` | StringSet | Individually blocked package names |
| `is_bricked` | Boolean | Whether device is currently bricked |
| `brick_everything` | Boolean | Block ALL apps (except system) |
| `paired_tag_id` | String | NFC tag UID paired to this device |
| `tag_name` | String | Display name for the paired tag |
| `app_groups` | String (JSON) | Custom group definitions |
| `selected_groups` | StringSet | Which groups are active |
| `group_pkgs_{id}` | StringSet | Customized packages per group |

### System Package Allowlist
These packages are NEVER blocked (in `EenthService.kt`):
- SystemUI, Samsung launcher, Samsung edge/cocktailbar
- Nexus launcher, Android settings
- Android system (chooser), Samsung resolver
- Samsung Tags service, Android NFC service
- Eenth itself

### NFC Flow
- **Pair:** `MainActivity` reads tag UID via `ReaderCallback`, saves to prefs, registers with Supabase
- **Brick/Unbrick:** Tap paired tag on `MainActivity` toggles `is_bricked`
- **Unbrick from blocker:** `BlockerActivity` has its own `ReaderCallback` for direct unbrick
- **System dispatch:** `NfcUnlockActivity` handles `TAG_DISCOVERED`/`TECH_DISCOVERED` intents

## Coding Conventions
- **No Jetpack Compose.** All UI is XML layouts inflated programmatically.
- Views are found via `findViewById` (viewBinding is enabled but not used for all layouts).
- Bottom sheets use `BottomSheetDialog` from Material library.
- Programmatic view creation is used for dynamic content (group detail grid, overlapping icons).
- `dpToPx()` helper in `MainActivity` for density-independent measurements.
- Colors defined in `res/values/colors.xml`. Dark theme only (`bg_primary=#050505`).
- Custom drawables in `res/drawable/bg_*.xml` for cards, hero sections, dots.

## Testing on Device
- After install, user must manually enable Accessibility Service: Settings → Accessibility → Eenth
- After `adb install`, accessibility service persists (no need to re-enable unless app data cleared)
- To unbrick a stuck device: `adb shell am force-stop com.eenth.blocker` or `adb shell pm clear com.eenth.blocker`
- NFC tag UID for dev tag: `044E5B1A1F1D91`

## Git Workflow
- `main` branch is the stable branch
- Feature branches: `feat/<feature-name>`
- PRs merged via GitHub
- Remote: `github.com/surpha/eenth`

## Common Pitfalls
- **ScrollView + RecyclerView:** Use `NestedScrollView`, never plain `ScrollView`
- **NFC system chooser blocked:** Android's system chooser/resolver packages must be in the allowlist or NFC dispatch breaks while bricked
- **SharedPreferences corruption:** Never use `sed` to edit prefs XML on device — use `pm clear` to reset
- **AccessibilityService context:** `EenthService` runs in its own process context; broadcast receivers need proper registration
- **Offline tag verification:** Currently not enforced (known limitation). Tag UID is verified locally against `paired_tag_id` pref.
