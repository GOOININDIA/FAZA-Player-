# FAZA Player

A modern, dark-themed Android video player built with Jetpack Compose and AndroidX Media3, featuring local library management, network streaming, deep player UI customization, and an integrated AI assistant.

---

## Key Features

### Playback & Media Engine
- **Media3 / ExoPlayer Core**: Smooth local and network video playback powered by AndroidX Media3 with `MediaSessionService` support.
- **Background Play & Picture-in-Picture (PiP)**: Continue listening in the background via a media foreground service or shrink the player into a floating PiP window.
- **Decoder & Aspect Ratio Controls**: Switch between hardware/software decoding modes and multiple aspect ratio presets.
- **Quick-Access Player Toolbar**: Instant access to Night Mode, Shuffle, Loop, Mute, Sleep Timer, A-B Repeat, Audio Effects, Equalizer presets, Playback Speed (`0.5x`–`2.0x`), Screenshot capture, Background Play, and Screen Rotation.
- **Gesture Controls**: Double-tap to skip and drag gestures with an on-screen HUD for brightness, volume, and seeking, plus a built-in gesture help overlay.

### Library, Search & Streaming
- **Local Video Library & Folder Browsing**: Scans and organizes device video folders with linked folder support, sorting options, and automatic video deduplication.
- **Search & Quick Filters**: Filter your media library rapidly using quick-filter chips and search queries.
- **Network Stream Playback**: Play remote video streams directly with built-in stream URL validation and protocol resolution.

### Customization & Adaptive UI
- **Material 3 & Dark-First Design**: Pitch-black (`#000000`) player interface with customizable accent presets, theme modes, and glassmorphic surfaces.
- **Deep Player UI Styling**: Customize seek bar styles, player control sizes and themes, Now Playing overlays, Running Time display modes, and App Icon overlays.
- **Subtitle Styling**: Configure subtitle appearance and edge styles for better readability.
- **Adaptive Layout**: Scales across phones, tablets, and foldables using width and height size classes.

### AI Assistant & Updates
- **Integrated AI Assistant**: Built-in chat assistant powered by Firebase AI with library context awareness, starter prompts, and persistent chat history.
- **In-App Update Checker**: Automatically checks for new builds via [GitHub Releases]([(https://github.com/GOOININDIA/FAZA-Player-/tags]) and the update manifest endpoint.

---

## App Specifications

| Property | Value |
| :--- | :--- |
| **App Name** | FAZA Player |
| **Version** | `1.48 Beta` (versionCode `50`) |
| **Application ID** | `apk.faza.go` |
| **Minimum Android** | Android 7.0 Nougat (API 24) |
| **Target / Compile SDK** | Android 16 (API 36) |
| **Supported ABIs** | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| **APK Size** | ~32 MB (`FAZA-Player-v1.48-Beta.apk`) |

---

## Tech Stack

- **Language & UI**: Kotlin, Jetpack Compose (`1.7.2`), Material 3 (`1.3.0`), Navigation Compose (`2.8.9`)
- **Media & Audio**: AndroidX Media3 (ExoPlayer & MediaSessionService)
- **Data & Persistence**: Room (`2.7.0`), DataStore Preferences (`1.1.7`), SQLite (`2.5.0`)
- **Architecture & Async**: Dagger (`2.57.2`), Kotlin Coroutines (`1.10.2`), ViewModel & Lifecycle (`2.8.7`)
- **AI & Services**: Firebase AI, Firebase App Check, Google Mobile Ads
- **Build Tooling**: Android Gradle Plugin (`9.1.1`)

---

## Permissions

| Permission | Purpose |
| :--- | :--- |
| `MANAGE_EXTERNAL_STORAGE` | Scan and access local video files and folders across device storage |
| `INTERNET` & `ACCESS_NETWORK_STATE` | Network stream playback, update checks, AI assistant, and ads |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Background media playback and persistent playback controls |
| `POST_NOTIFICATIONS` | Media playback notifications and update alerts |
| `WAKE_LOCK` | Prevent screen/CPU sleep during active video playback |
| `com.google.android.gms.permission.AD_ID` | Google Mobile Ads integration |

---

## Installation

1. Download `FAZA-Player-v1.48-Beta.apk` from the [Releases](https://github.com/GOOININDIA/FAZA-Player/releases) page.
2. Open the downloaded `.apk` file on your Android device (Android 7.0 or higher).
3. If prompted, allow **Install from unknown sources** for your browser or file manager.
4. Launch **FAZA Player** and grant **All File Access** permission to load your local video library.

---

## Community & Support

- **GitHub**: [GOOININDIA/FAZA-Player](https://github.com/GOOININDIA/FAZA-Player)
- **Telegram**: [t.me/fazaplayer](https://telegram.me/fazaplayer)
- **Instagram**: [@heyrajbir_](https://www.instagram.com/heyrajbir_)
- **Support Development**: [Buy Me a Coffee](https://buymeacoffee.com/FGa6Tc98UN)
- 
