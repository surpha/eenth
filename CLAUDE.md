# CLAUDE.md — Agent Instructions for Block (formerly Eenth)

## What is Block?
Block is an open-source, physical-first Android app blocker. Users pair an NFC tag ("block") to their phone. Tapping the tag blocks/unblocks their device — blocking all selected apps with a full-screen overlay. No willpower needed; you must physically find your block to get your apps back.

**Brand identity:** The app is called "Block". When blocked, the brand text transitions to "BLOCKIN" (reads as "be lockin' in"). The tagline is "BLOCK IN".

## Tech Stack
- **Language:** Kotlin (JVM target 11)
- **UI:** Standard Android XML Layouts (NOT Compose) with AppCompat + Material Design
- **Storage:** SharedPreferences (`eenth_prefs`)
- **Blocking engine:** ForegroundService + UsageStatsManager polling + TYPE_APPLICATION_OVERLAY
- **NFC:** `NfcAdapter.ReaderCallback` for tag read
- **Server:** Supabase REST API (tag registration/verification)
- **HTTP client:** OkHttp 4.12.0
- **Charts:** MPAndroidChart v3.1.0 (via JitPack)
- **Build:** Gradle with version catalog (`gradle/libs.versions.toml`)
- **Min SDK:** 26, Target/Compile SDK: 36
- **Website:** React + Vite landing page (in `website/` folder)

## Build & Deploy
```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
$HOME/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# If multiple devices, specify serial:
$HOME/Library/Android/sdk/platform-tools/adb -s <SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
$HOME/Library/Android/sdk/platform-tools/adb shell am start -n com.eenth.blocker/.SplashActivity
```

## Project Structure
```
app/src/main/java/com/eenth/blocker/
├── SplashActivity.kt        # Animated splash: open lock → BLOCK → closed lock → BLOCKIN
├── MainActivity.kt          # Main UI — status card, tag section, groups, app list, bottom nav
├── StatsActivity.kt         # Insights screen — MPAndroidChart (focus, screen time, pickups, top apps)
├── BlockMonitorService.kt   # ForegroundService — polls foreground app, shows/hides overlay
├── BlockerActivity.kt       # NFC unblock screen — launched from overlay tap
├── NfcUnlockActivity.kt     # Handles system NFC intent dispatch (TAG_DISCOVERED)
├── EenthTile.kt             # Quick Settings tile for block status
├── TagRepository.kt         # Supabase REST client for tag pair/verify/unpair/updateName
├── AppGroup.kt              # AppGroup data class + GroupManager (presets + custom groups)
├── GroupAdapter.kt           # RecyclerView adapter for group cards (overlapping app icons)
├── AppListAdapter.kt         # RecyclerView adapter for the app list (icon + name + switch)
└── EenthService.kt          # (Legacy) AccessibilityService — kept for reference, disabled in manifest
└── ui/theme/                 # Color, Theme, Type (unused Compose leftovers)

website/                      # React + Vite landing page
├── src/components/           # Hero, BuySection, FAQ, HowItWorks, Modes, etc.
├── src/pages/                # Page routes
└── public/                   # Static assets
```

## Key Concepts

### App Launch Flow
1. `SplashActivity` — Animated 2s splash (open lock + "BLOCK" → closed lock + "BLOCKIN" in red)
2. `MainActivity` — Main configuration and status screen
3. Bottom nav switches between Home (MainActivity) and Insights (StatsActivity)

### Blocking Flow
1. `BlockMonitorService` (ForegroundService) polls foreground app every 500ms via `UsageStatsManager` events
2. Checks if app is in: `blocked_apps` OR any selected group's packages OR `brick_everything` is on
3. If blocked + bricked → shows full-screen `TYPE_APPLICATION_OVERLAY` ("B LOCKED IN" screen)
4. User taps overlay → opens `BlockerActivity` → taps NFC tag to unblock
5. Overlay is dismissed when `is_bricked` becomes false

### SharedPreferences Keys (`eenth_prefs`)
| Key | Type | Purpose |
|-----|------|---------|
| `blocked_apps` | StringSet | Individually blocked package names |
| `is_bricked` | Boolean | Whether device is currently blocked |
| `brick_everything` | Boolean | Block ALL apps (except system + payments) |
| `paired_tag_id` | String | NFC tag UID paired to this device |
| `tag_name` | String | Display name for the paired tag |
| `app_groups` | String (JSON) | Custom group definitions |
| `selected_groups` | StringSet | Which groups are active |
| `group_pkgs_{id}` | StringSet | Customized packages per group |
| `brick_start_time` | Long | Timestamp when current session started |
| `today_focus_ms` | Long | Accumulated focus time today (ms) |
| `today_sessions` | Int | Number of block sessions today |
| `stat_date` | String | Date for daily stats reset (yyyy-MM-dd) |
| `focus_YYYY-MM-DD` | Long | Archived daily focus time for weekly chart |

### System Package Allowlist
These packages are NEVER blocked (in `BlockMonitorService.kt`):
- SystemUI, Samsung launcher, Samsung edge/cocktailbar
- Nexus launcher, Android settings
- Android system (chooser), Samsung resolver
- Samsung Tags service, Android NFC service
- Block itself (`com.eenth.blocker`)

### Payment App Allowlist
These are never blocked even in "brick everything" mode:
- Google Pay, Paytm, PhonePe, UPI, Amazon, MobiKwik, Samsung Pay, Play Store

### NFC Flow
- **Pair:** `MainActivity` reads tag UID via `ReaderCallback`, saves to prefs, registers with Supabase
- **Block/Unblock:** Tap paired tag on `MainActivity` toggles `is_bricked`
- **Unblock from blocker:** `BlockerActivity` has its own `ReaderCallback` for direct unblock
- **System dispatch:** `NfcUnlockActivity` handles `TAG_DISCOVERED`/`TECH_DISCOVERED` intents

### Insights / Stats
- `StatsActivity` uses **MPAndroidChart** for 4 chart types:
  1. **Focus Time** (BarChart, 7 days) — green bar for today
  2. **Screen Time** (stacked BarChart, 7 days) — per-app colored breakdown with legend
  3. **Pickups** (BarChart, 7 days) — orange bar for today
  4. **Top Apps** (HorizontalBarChart) — top 6 apps by screen time today
- Uses `UsageStatsManager` for screen time + pickup count
- Focus data from SharedPreferences (archived daily on unblock)
- Streak calculation based on consecutive days with focus sessions

## Coding Conventions
- **No Jetpack Compose.** All UI is XML layouts inflated programmatically.
- Views are found via `findViewById` (viewBinding is enabled but not used for all layouts).
- Bottom sheets use `BottomSheetDialog` from Material library.
- Programmatic view creation is used for dynamic content (group detail grid, overlapping icons).
- `dpToPx()` helper in `MainActivity` for density-independent measurements.
- Colors defined in `res/values/colors.xml`. Dark theme only (`bg_primary=#000000`).
- Custom drawables in `res/drawable/bg_*.xml` for cards, hero sections, dots.
- Brand text: "BLOCK" (unblocked, white) / "BLOCKIN" (blocked, red #FF453A)
- Bottom nav on both main screens with vector icons (home, bar chart)

## Color Scheme
| Token | Hex | Usage |
|-------|-----|-------|
| `bg_primary` | #000000 | App background |
| `bg_card` | #0A0A0A | Card backgrounds |
| `bg_card_elevated` | #111111 | Elevated cards |
| `text_primary` | #FFFFFF | Main text |
| `text_secondary` | #8E8E93 | Secondary text |
| `text_tertiary` | #48484A | Muted text, inactive nav |
| `accent_red` | #FF453A | Blocked state, active brand |
| `accent_green` | #32D74B | Unblocked state, focus charts |
| `accent_orange` | #FF9F0A | Warnings, pickups chart |
| `divider` | #1C1C1E | Borders, grid lines |

## Testing on Device
- After install, grant "Display over other apps" permission: Settings → Apps → Block → Display over other apps
- Grant "Usage access" permission: Settings → Apps → Special access → Usage access
- The BlockMonitorService starts automatically when the app launches
- To unblock a stuck device: `adb shell am force-stop com.eenth.blocker` or `adb shell pm clear com.eenth.blocker`
- NFC tag UID for dev tag: `044E5B1A1F1D91`

## Git Workflow
- `main` branch is the stable branch
- Feature branches: `feat/<feature-name>`
- PRs merged via GitHub
- Remote: `github.com/surpha/eenth`

## Common Pitfalls
- **ScrollView + RecyclerView:** Use `NestedScrollView`, never plain `ScrollView`
- **NFC system chooser blocked:** Android's system chooser/resolver packages must be in the allowlist or NFC dispatch breaks while blocked
- **SharedPreferences corruption:** Never use `sed` to edit prefs XML on device — use `pm clear` to reset
- **AccessibilityService context:** `EenthService` runs in its own process context; broadcast receivers need proper registration
- **Offline tag verification:** Currently not enforced (known limitation). Tag UID is verified locally against `paired_tag_id` pref.
- **MPAndroidChart Legend:** Use `isWordWrapEnabled` (not `wordWrapEnabled`) for legend text wrapping
- **Stacked BarChart:** Use `BarEntry(x, floatArrayOf(...))` with `stackLabels` on the dataset
- **Usage permission:** `UsageStatsManager` queries return empty without PACKAGE_USAGE_STATS + user grant
- **Package name:** Internal package is still `com.eenth.blocker` (not renamed to avoid breaking installs)
