<!--
  Replace prlancas with your GitHub username/org (e.g. paul-lancaster) throughout
  this file so the badges and links below resolve correctly.
-->

# BadgeBot

[![CI](https://github.com/prlancas/badgebot/actions/workflows/ci.yml/badge.svg)](https://github.com/prlancas/badgebot/actions/workflows/ci.yml)
[![Release](https://github.com/prlancas/badgebot/actions/workflows/release.yml/badge.svg)](https://github.com/prlancas/badgebot/actions/workflows/release.yml)
[![Latest release](https://img.shields.io/github/v/release/prlancas/badgebot?sort=semver)](https://github.com/prlancas/badgebot/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A small, focused Android app that drives a Bluetooth LE robot using a simple
directional control pad. It connects to any **Bluefruit UART-capable**
peripheral (Nordic UART Service) and sends the same button commands as the
Adafruit Bluefruit Connect "Control Pad" — but stripped down to just the four
directional arrows.

> Downloads: grab the latest APK / Play Store bundle from the
> [**Releases page**](https://github.com/prlancas/badgebot/releases/latest).

## Features

- Scan for and connect to UART-capable BLE peripherals (Nordic UART Service).
- A clean, Material 3 control pad with **up / down / left / right** arrows only.
  Up = forward, Down = backward, Left = turn left, Right = turn right.
- Sends press **and** release events so the robot can start/stop on hold.
- Hamburger menu to switch between screens:
  - **Control Pad** — the driving arrows.
  - **Serial Console** — view live TX/RX traffic and type/send raw messages.
  - **Print Marker** — generate and print/share an ArUco (`DICT_4X4_50`) fiducial.
  - **Camera & Path** — point the camera at the printed marker, tap to draw a
    ground path pinned to the marker, then auto-drive the robot along it.
- **Record serial traffic** toggle that works on any screen and exports a
  timestamped transcript via the share sheet.
- Works in portrait and landscape (rotation supported).
- Runtime Bluetooth permission handling for Android 8.0 (API 26) → 15 (API 35).
- Light/dark theme with dynamic color on Android 12+.

## How it works

The app speaks the Adafruit Bluefruit "Controller" protocol over the Nordic
UART Service:

| Item | Value |
| --- | --- |
| Service UUID | `6e400001-b5a3-f393-e0a9-e50e24dcca9e` |
| TX characteristic (phone → robot) | `6e400002-b5a3-f393-e0a9-e50e24dcca9e` |
| RX characteristic (robot → phone) | `6e400003-b5a3-f393-e0a9-e50e24dcca9e` |

Each button sends an ASCII command followed by a one-byte checksum:

```
!B <tag> <state> <crc>
```

- `state` — `1` while held, `0` on release
- `crc` — bitwise-inverted 8-bit sum of the preceding bytes

The stock Bluefruit numbering is `5` up, `6` down, `7` left, `8` right, but on
this robot those tags map to different motions. BadgeBot therefore maps each
on-screen arrow to the tag that produces the **intuitive** movement:

| Arrow | Motion | Tag sent |
| --- | --- | --- |
| Up | forward | `7` |
| Down | backward | `8` |
| Left | turn left | `5` |
| Right | turn right | `6` |

For example, pressing **Up** sends the bytes `!B71` followed by its CRC. The
packet-building logic lives in
[`UartProtocol.kt`](app/src/main/java/com/badgebot/controller/ble/UartProtocol.kt)
/ [`ControlButton.kt`](app/src/main/java/com/badgebot/controller/ble/ControlButton.kt)
and is covered by unit tests.

## Camera & Path (marker-anchored AR)

The **Camera & Path** screen uses the phone camera (CameraX) plus OpenCV ArUco
detection to follow a path:

1. Print marker **#0** (from the *Print Marker* screen) and place it flat where
   the robot starts, oriented so the marker's "up" is the robot's forward.
2. Open **Camera & Path** and point the camera at the marker — the printed
   marker is the world anchor, so the drawn path stays pinned to the ground
   while the marker is in view.
3. Tap the ground to add waypoints, then press **Drive path**.

Path following is **open-loop** (no position feedback): waypoints are converted
to timed forward/turn arrow commands by
[`PathPlanner`](app/src/main/java/com/badgebot/controller/path/PathPlanner.kt)
and executed by
[`PathDriver`](app/src/main/java/com/badgebot/controller/path/PathDriver.kt).
Two things need calibrating on the real robot for accurate driving:

- **`DriveTuning`** — the robot's forward speed (m/s) and turn rate (rad/s).
- **Marker size** — `ArucoGroundTracker(markerLengthMeters = …)` must match the
  printed marker's real side length so distances are to scale.

The homography and path-planning maths are pure Kotlin and unit-tested; the
camera registration and tuning constants are the parts that require the device
and robot. (The build ships `arm64-v8a` only to keep the OpenCV native payload
small — add ABIs in `app/build.gradle.kts` for other devices.)

## Requirements

- Android Studio Ladybug (or newer) / command-line Gradle
- JDK 17
- Android SDK Platform 35, Build Tools 35
- A device running Android 8.0 (API 26) or newer with Bluetooth LE

## Building

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (signed if a keystore is configured — see below)
./gradlew assembleRelease
# → app/build/outputs/apk/release/

# Play Store bundle (AAB)
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

## Testing

```bash
# Fast JVM unit tests (protocol, CRC, models)
./gradlew testDebugUnitTest

# Instrumented UI tests (requires a connected device/emulator)
./gradlew connectedDebugAndroidTest
```

## Signing for release

Release signing is **optional for local builds** — without a key the release
build produces an unsigned artifact so CI still works. To produce a
Play-Store-ready signed build, provide your upload key in one of two ways.

### Local builds — `keystore.properties`

Create a `keystore.properties` file in the project root (it is git-ignored):

```properties
storeFile=/absolute/path/to/upload-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

### CI builds — GitHub Actions secrets

The [release workflow](.github/workflows/release.yml) reads these repository
secrets (**Settings → Secrets and variables → Actions**):

| Secret | Description |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Your `.jks` keystore, base64-encoded |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Encode your keystore with:

```bash
base64 -i upload-keystore.jks | pbcopy   # macOS
base64 -w0 upload-keystore.jks           # Linux
```

## Continuous integration & releases

- **CI** ([`ci.yml`](.github/workflows/ci.yml)) runs on every push and PR: it
  executes the unit tests and builds the debug APK, uploading both the APK and
  the test report as artifacts.
- **Release** ([`release.yml`](.github/workflows/release.yml)) runs when you
  push a version tag (e.g. `v1.0.0`). It builds the signed release **APK** and
  **AAB**, attaches them to a GitHub Release, and auto-generates release notes.

Cutting a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The resulting artifacts appear on the
[Releases page](https://github.com/prlancas/badgebot/releases). Upload the `.aab`
to the Google Play Console.

## Project structure

```
app/src/main/java/com/badgebot/controller/
├── MainActivity.kt            # Entry point, permission handling
├── ble/
│   ├── BleConstants.kt        # Nordic UART Service UUIDs
│   ├── BleUartManager.kt      # Scan / connect / write over GATT
│   ├── ControlButton.kt       # Direction → protocol tag mapping
│   ├── UartProtocol.kt        # Command + CRC packet building (tested)
│   └── ...
└── ui/
    ├── BadgeBotApp.kt         # Scan ↔ Control Pad navigation
    ├── ScanScreen.kt          # Device discovery UI
    ├── ControlPadScreen.kt    # The four-arrow control pad
    └── theme/                 # Material 3 theme
```

## Acknowledgements

The BLE UART protocol and control-pad command format follow Adafruit's
[Bluefruit LE Connect](https://github.com/adafruit/Bluefruit_LE_Connect_Android_V4)
app.

## License

Released under the [MIT License](LICENSE).
