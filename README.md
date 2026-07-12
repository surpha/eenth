# Eenth

An open-source Android app blocker that uses NFC. Pair a physical tag to your phone, tap it to brick your device — all selected apps get blocked behind a full-screen wall. Tap again to unbrick. No timers, no workarounds, no willpower needed.

## How it works

1. Pair any NFC tag to your phone
2. Pick which apps to block (individually or by group)
3. Tap your tag to brick — blocked apps become inaccessible
4. Tap again to unbrick

The blocking runs through an AccessibilityService that detects when a blocked app opens and immediately covers it with a full-screen blocker. The only way to dismiss it is to physically tap your NFC tag.

## Features

- **NFC brick/unbrick** — one tap to lock, one tap to unlock
- **Block everything mode** — bricks all non-system apps at once
- **App groups** — presets for Social Media, Streaming, Messaging, Games, Shopping. Fully customizable (add/remove apps from any group)
- **Individual app blocking** — toggle specific apps on/off
- **Unbrick from blocker** — tap your tag directly on the blocker screen without going home first
- **Tag naming** — give your NFC tag a custom name, synced to the server
- **Server-backed pairing** — tag registrations stored in Supabase so one tag can't be paired to multiple devices

## Tech

- Kotlin, standard Android XML layouts (no Compose)
- SharedPreferences for all local state
- AccessibilityService for app detection
- NfcAdapter.ReaderCallback for tag reads
- OkHttp + Supabase REST API for tag registration
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

1. Open Eenth
2. Go to Settings → Accessibility → Eenth and enable the service
3. Tap "Pair a tag" and hold an NFC tag to your phone
4. Select apps or groups to block
5. Tap your tag to brick

## Unbrick a stuck device

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
├── MainActivity.kt       — main UI, NFC pairing, app/group management
├── EenthService.kt       — AccessibilityService, blocking engine
├── BlockerActivity.kt    — full-screen blocker + NFC unbrick
├── NfcUnlockActivity.kt  — handles system NFC intents
├── TagRepository.kt      — Supabase REST client
├── AppGroup.kt           — group definitions + GroupManager
├── GroupAdapter.kt        — group card adapter (overlapping app icons)
└── AppListAdapter.kt      — app list adapter
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed system design.

## Known limitations

- Tag verification is local-only — the app checks the tag UID against SharedPreferences, not the server, when bricking/unbricking. This means offline pairing bypass is theoretically possible.
- Accessibility service must be manually enabled after install.
- Some OEM launchers may need to be added to the system package allowlist in `EenthService.kt`.

## License

MIT
