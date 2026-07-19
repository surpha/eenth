# Block

An open-source Android app blocker that uses NFC. Pair a physical tag ("block") to your phone, tap it to block your device — all selected apps get blocked behind a full-screen overlay. Tap again to unblock. No timers, no workarounds, no willpower needed.

## How it works

1. Pair any NFC tag to your phone
2. Pick which apps to block (individually or by mode/group)
3. Tap your tag to block — selected apps become inaccessible
4. Tap again to unblock

The blocking runs through a ForegroundService that polls the foreground app every 500ms using UsageStatsManager. When a blocked app is detected, a full-screen overlay covers it instantly. The only way to dismiss it is to physically tap your NFC tag.

## Features

- **NFC block/unblock** — one tap to lock, one tap to unlock
- **Block everything mode** — blocks all non-system apps at once
- **App groups (Modes)** — presets for Social Media, Streaming, Messaging, Games, Shopping. Displayed as a 2-column tile grid. Fully customizable.
- **Individual app blocking** — toggle specific apps on/off
- **Settings locked while blocked** — can't change modes, apps, or toggles until unblocked
- **Unblock from overlay** — NFC reader mode active on the blocked screen (no system chooser)
- **Tag naming** — give your NFC tag a custom name, synced to the server
- **Server-backed pairing** — tag registrations stored in Supabase so one tag can't be paired to multiple devices
- **Insights** — focus time, screen time, pickups, top apps (7-day charts)

## Tech

- Kotlin, standard Android XML layouts (no Compose)
- Inter font family (Thin → Bold) for iOS-like typography
- SharedPreferences for all local state
- ForegroundService + UsageStatsManager + TYPE_APPLICATION_OVERLAY for blocking
- NfcAdapter.ReaderCallback for tag reads
- OkHttp + Supabase REST API for tag registration
- MPAndroidChart for insights charts
- Min SDK 26, Target SDK 36

## Building

```bash
./gradlew assembleDebug
```

Install to a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Setup after install

1. Open Block
2. Grant "Display over other apps" permission when prompted
3. Grant "Usage access" (Settings → Apps → Special access → Usage access)
4. Tap "Pair a tag" and hold an NFC tag to your phone
5. Select apps or modes to block
6. Tap your tag to block

## Unblock a stuck device

If you're locked out and don't have your tag:

```bash
adb shell am force-stop com.eenth.blocker
```

Or to fully reset:

```bash
adb shell pm clear com.eenth.blocker
```

## Project structure

```
app/src/main/java/com/eenth/blocker/
├── SplashActivity.kt       — animated splash (BLOCK → B LOCKIN)
├── MainActivity.kt         — main UI, NFC pairing, app/group management
├── BlockMonitorService.kt  — ForegroundService, polls foreground app, shows overlay
├── BlockerActivity.kt      — NFC unblock screen (reader mode active)
├── NfcUnlockActivity.kt    — handles system NFC intents
├── EenthTile.kt            — Quick Settings tile
├── TagRepository.kt        — Supabase REST client
├── AppGroup.kt             — group definitions + GroupManager
├── GroupAdapter.kt          — 2-column grid adapter for mode tiles
├── StatsActivity.kt        — Insights screen (charts)
└── EenthService.kt         — (Legacy) AccessibilityService, disabled
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed system design.

## Known limitations

- Tag verification is local-only — the app checks the tag UID against SharedPreferences, not the server, when bricking/unbricking. This means offline pairing bypass is theoretically possible.
- Accessibility service must be manually enabled after install.
- Some OEM launchers may need to be added to the system package allowlist in `EenthService.kt`.

## License

MIT
