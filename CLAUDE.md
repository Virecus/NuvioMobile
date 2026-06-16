# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Nuvio** is a modern media hub for Android and iOS built with **Kotlin Multiplatform** and **Compose Multiplatform**. It's a rewrite of the original React Native app with a shared codebase powering both platforms.

Key features:
- Stremio addon ecosystem integration
- Playback-focused experience with Media3 (Android) and AVFoundation (iOS)
- Collection tools and watch progress tracking
- Downloads and offline support
- Cross-platform UI via Compose Multiplatform

## Architecture

### Multiplatform Structure

The project uses **Kotlin Multiplatform (KMP)** with modular source sets:

```
composeApp/src/
├── commonMain/           # Shared UI, repositories, and platform-agnostic logic
├── commonTest/           # Shared tests
├── androidMain/          # Android-specific integrations
├── androidFull/          # Full-featured Android variant (includes plugins)
├── androidPlaystore/     # Play Store variant (limited features)
├── iosMain/              # iOS-specific integrations
├── iosFull/              # Full-featured iOS variant
├── iosAppStore/          # App Store variant (limited features)
├── fullCommonMain/       # Shared code for full variants only
└── desktopMain/          # Desktop support (WIP)
```

**Key principle**: Code in `commonMain` must compile for all platforms. Platform-specific code lives in `androidMain`/`iosMain`. Features exclusive to full builds go in `fullCommonMain` with conditional expect/actual implementations.

### Feature Organization

Features live in `composeApp/src/commonMain/kotlin/com/nuvio/app/features/` organized by domain:

- `addons/` - Stremio addon runtime and manifest parsing
- `player/` - Playback, subtitles, skip markers (IntroDb)
- `collection/` - Library, watchlist, bookmarks
- `watchprogress/` - Resume state, watched progress
- `debrid/` - Debrid (Premiumize, Real Debrid) provider integrations
- `streams/` - Source/stream selection and quality handling
- `home/` - Home screen and featured content
- `settings/` - User preferences and configuration
- Other domains: `search`, `details`, `trakt`, `tmdb`, `profiles`, `plugins`, `notifications`, etc.

### Core Layers

**composeApp/src/commonMain/kotlin/com/nuvio/app/core/**
- `ui/` - Reusable Compose components and theming
- `build/` - Build-time config (version, feature flags)
- `i18n/` - Localization strings and formatting
- `network/` - HTTP client setup, API base URLs
- `format/` - Date, time, size formatting utilities

## Build & Development

### Build Commands

**Android debug build:**
```bash
./gradlew :composeApp:assembleDebug          # Full variant (plugins enabled)
./gradlew :composeApp:assemblePlaystoreDebug # Play Store variant
```

**Android release build:**
```bash
./gradlew :composeApp:assembleFullRelease    # Full variant
```

Requires keystore config in `local.properties`:
```
NUVIO_RELEASE_STORE_FILE=path/to/keystore.jks
NUVIO_RELEASE_STORE_PASSWORD=...
NUVIO_RELEASE_KEY_ALIAS=...
NUVIO_RELEASE_KEY_PASSWORD=...
```

**iOS build:**
```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./scripts/build-distribution.sh
```

**Clean build (when cache is stale):**
```bash
./gradlew clean assembleFullDebug
```

### Versioning

Version is the **single source of truth** and lives in `iosApp/Configuration/Version.xcconfig`:
```
MARKETING_VERSION=0.2.5
CURRENT_PROJECT_VERSION=77
```

These values are read at build-time by `composeApp/build.gradle.kts` and embedded in `AppVersionConfig.kt` for both Android and iOS.

### API Configuration

**Build-time configuration** is injected via `local.properties` (git-ignored for security):

```properties
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
TRAKT_CLIENT_ID=...
TRAKT_CLIENT_SECRET=...
INTRODB_API_URL=...
IMDB_RATINGS_API_BASE_URL=...
PREMIUMIZE_CLIENT_ID=...
MDBLIST_API_KEY=...
```

Config values are generated into `GenerateRuntimeConfigsTask` (in build.gradle.kts), creating Kotlin objects like `SupabaseConfig.kt`, `TraktConfig.kt`, etc. at compile time. **Never commit API keys**—they're `.gitignore`'d by design.

### Running on Device/Emulator

**Install and run on Android emulator/device:**
```bash
adb install -r composeApp/build/outputs/apk/full/debug/composeApp-full-debug.apk
adb shell am start -n com.nuvio.app/.MainActivity
```

## Key Technologies

- **Kotlin Multiplatform** - Shared business logic across Android/iOS
- **Compose Multiplatform** - Shared UI framework
- **AndroidX Media3** - Android playback engine
- **AVFoundation** - iOS native video/audio
- **Supabase** - Backend for user data (library, watched state, etc.)
- **Trakt** - Third-party watched state and ratings
- **Stremio Addons** - Plugin ecosystem for content sources

## Contributing Guidelines

See `CONTRIBUTING.md` for strict rules on PRs. Key points:

- **Bug fixes only** for reported issues with reproducible steps
- **UI PRs** require: linked issue, before/after screenshots, smallest possible change
- **Behavior changes** need: linked approved feature request or bug fix
- **Large changes** must be approved via feature request issue first
- **No cosmetic changes**, refactors, or architecture changes without prior approval
- **Translation PRs** are allowed if focused on localization only

## Testing

Tests live in `composeApp/src/commonTest/` and use JUnit + assertions. Run with:

```bash
./gradlew :composeApp:testDebugUnitTest
```

## Feature Flags & Build Variants

The app has two main build variants:
- **Full** - Includes plugin runtime and advanced features
- **PlayStore/AppStore** - Limited features for store compliance

Conditional code uses `expect`/`actual` in `fullCommonMain` and `commonMain` to gate features. Check `AppFeaturePolicy` for runtime feature flags.

## Common Patterns

### Platform-Specific Code

Use `expect` in `commonMain` and `actual` in platform-specific source sets:

```kotlin
// commonMain
expect fun getPlatformName(): String

// androidMain
actual fun getPlatformName() = "Android"

// iosMain
actual fun getPlatformName() = "iOS"
```

### Storage & Preferences

Platform abstractions for SharedPreferences (Android) and UserDefaults (iOS) live in feature packages. Example: `ProfileStorage`, `ThemeSettingsStorage`.

### Coroutines

The app uses `kotlinx.coroutines` for async work with `Dispatchers.IO` for network/disk and `Dispatchers.Main` for UI updates. Lifecycle-aware collection via `collectAsStateWithLifecycle()`.

### HTTP Client

Uses OkHttp via `httpRequestRaw()` utility in `features/addons/` for raw HTTP. Higher-level APIs use Supabase client or custom repositories.

## Update System

In-app updates check GitHub releases via `AppUpdater.kt`:
- Fetches from GitHub API: `repos/Virecus/NuvioMobile/releases`
- Filters for releases matching branch: `cmp-rewrite` (in tag or `target_commitish`)
- Compares versions using `VersionUtils.isRemoteNewer()`
- Downloads APK and prompts user to install

Release tag format: `0.2.5-cmp-rewrite` for the main branch.
