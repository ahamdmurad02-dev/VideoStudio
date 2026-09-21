# VideoStudio

Native Android video recording and editing app built with Kotlin and Jetpack Compose.

**Official website:** https://videostudio-zeta.vercel.app/

VideoStudio records the screen or camera and edits clips on-device. Processing stays local. There are no ads, no tracking SDKs, and no required accounts.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android_7.0%2B_(API_24%2B)-brightgreen.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Features

### Recording
- Foreground screen recording at 1080p / 720p / 480p, up to 60 FPS
- Optional microphone audio
- In-app camera capture with CameraX
- Persistent recording notification with stop / status controls

### Editor
- Trim and split clips on a timeline
- Chroma key (green / blue screen) with adjustable tolerance
- Color filters: Warm, Cool, Vintage, Cinematic B&W, High Contrast, Vibrant
- Brightness, contrast, and saturation sliders
- Audio boost up to 200%
- Aspect ratios for Reels / TikTok (9:16), YouTube (16:9), Instagram (1:1), and 4:5
- On-device MP4 export

### Privacy
- Offline processing on the device
- No ads or analytics SDKs
- No login or subscriptions
- Permissions limited to camera, microphone, media, and screen capture

## Website

Product site and downloads: **https://videostudio-zeta.vercel.app/**

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Media3, MediaMuxer, MediaCodec, CameraX
- Room for local video metadata
- MVVM with Coroutines and StateFlow
- Gradle Kotlin DSL and version catalog

Package ID: `com.ahmad.videostudio`

## Requirements

- Android Studio Ladybug / recent stable or newer
- JDK 17 or 21
- Android SDK API 36 (minSdk 24)

## Build

```bash
git clone https://github.com/ahamdmurad02-dev/VideoStudio.git
cd VideoStudio
chmod +x gradlew
./gradlew assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

Signed release (use your own keystore; do not commit it):

```bash
export KEYSTORE_PATH="/path/to/your/upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"
export KEY_ALIAS="upload"

./gradlew assembleRelease
```

If those environment variables are not set, Gradle builds an unsigned release variant locally. Do not place keystores or passwords in the repository.

## Project layout

```
VideoStudio/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── data/          # Room database, DAO, repository
│   │   ├── media/         # Screen recording service, export, thumbnails
│   │   ├── python/        # Embedded helper models / processor
│   │   ├── ui/            # Camera, recorder, editor, library, settings
│   │   └── MainActivity.kt
│   └── build.gradle.kts
├── gradle/                # Version catalog and wrapper
├── .github/workflows/     # Android CI
├── README.md
└── LICENSE
```

## Security notes

This repository does **not** include:

- API keys or `.env` files with live secrets
- Keystores (`.jks` / `.keystore`)
- Signed APKs or Play signing files
- `google-services.json` or `local.properties`

Use `.env.example` as a template if you add optional Gemini / Firebase keys locally.

## License

MIT License. See [LICENSE](LICENSE).
