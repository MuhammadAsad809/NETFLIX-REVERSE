# Netflix Native Clone & Streaming Engine

A 100% native Android application built with Jetpack Compose, ExoPlayer (Media3), and official Netflix UI assets extracted via static forensic analysis from the official Netflix APK (`netflix-9-90-0-build-10-74793.apk`).

Connected with native Kotlin high-speed MovieBox streaming engine ported from `moviebox_server.py` and `moviebox_downloader.py`.

---

## 🚀 Key Features

### 1. Authentic Netflix Design System (Hawkins UI)
- **Official Brand Assets**: Extracted official Netflix app icons across all mipmap densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
- **Official Splash Screen**: Android 12+ `Theme.SplashScreen` system integration with authentic `glowing-netflix-n-logo.json` Lottie animation and official studio "Ta-dum" chime playback via Android `SoundPool`.
- **Hawkins Badges**: Dolby Atmos, Dolby Vision, Spatial Audio, Audio Description, SDH vector drawables extracted directly from the APK.
- **Micro-Interactions**: Lottie animations for *My List* checkmark toggle, *Rating thumbs up/down*, *Download NUX*, and *Buffer spinner*.
- **Hero Billboard & Feed Rows**: Netflix-styled auto-featured header, horizontal scrolling category carousels (Trending Now, Top 10 Today, Bollywood & Hindi, Sci-Fi & Action), and shimmer loading placeholders.

### 2. High-Performance Native Video Player (Media3 / ExoPlayer)
- Landscape fullscreen video playback with touch gestures.
- Custom Netflix HUD overlay with Play/Pause, 10s Forward/Replay buttons, seekbar, and stream resolution indicators (4K / 1080p / 720p).
- Lottie loading spinner on video buffering.
- Audio sonification feedback (tick and pop sound effects).

### 3. MovieBox Streaming & Download Engine
- **Direct Kotlin Port**: Ported client token hashing (MD5 salted string generation) directly from `moviebox_server.py`.
- **Search & Categorization**: Async OkHttp + Gson multi-category feeds with fallback feeds.
- **Direct MP4 Stream Extraction**: Extracts direct HLS/MP4 CDN streams from MovieBox subject URLs.
- **Background Downloader**: Multi-threaded chunk downloader saving video files directly to shared external storage (`/storage/emulated/0/Download/MovieBox/`).

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9.23
- **UI Toolkit**: Jetpack Compose (BOM 2024.05.00) + Material 3
- **Media Engine**: AndroidX Media3 (ExoPlayer 1.3.1)
- **Image Loading**: Coil Compose 2.6.0
- **Animation**: Airbnb Lottie Compose 6.4.0
- **Networking**: OkHttp 4.12.0 + Gson 2.10.1 + Kotlin Coroutines
- **Target SDK**: Android 14 (API 34) | **Min SDK**: Android 8.0 (API 26)

---

## 🏗️ Building with GitHub Actions CI/CD

The project includes an automated GitHub Actions workflow (`.github/workflows/build.yml`) that compiles the debug APK in an Ubuntu runner and uploads it as an artifact.

### How to Get the APK:
1. Navigate to the **Actions** tab in your GitHub repository:
   `https://github.com/MuhammadAsad809/NETFLIX-REVERSE/actions`
2. Select the latest run of **Build Netflix Native Android APK**.
3. Once completed (green checkmark), scroll down to the **Artifacts** section at the bottom.
4. Click on `netflix-native-debug-apk` to download the zip containing `app-debug.apk`.
5. Install on your Android device!

You can also trigger a fresh build manually at any time by clicking **Run workflow** on the Actions tab.

---

## 📁 Repository Structure

```
NetflixNative/
├── .github/workflows/
│   └── build.yml              # GitHub Actions build & artifact pipeline
├── app/
│   ├── src/main/
│   │   ├── assets/lottiefiles # Extracted Netflix Lottie micro-interactions
│   │   ├── java/com/netflix/moviebox/
│   │   │   ├── api/           # MovieBoxClient.kt (Token gen & stream extract)
│   │   │   ├── data/          # MovieBoxRepository.kt (Category feeds)
│   │   │   ├── downloader/    # MovieDownloader.kt (Download manager)
│   │   │   ├── ui/
│   │   │   │   ├── components/ # HeroBillboard, MovieRow, DetailBottomSheet, Shimmer
│   │   │   │   ├── player/    # PlayerActivity (ExoPlayer), SoundManager (SoundPool)
│   │   │   │   ├── screens/   # SplashScreen, HomeScreen, SearchScreen, DownloadsScreen
│   │   │   │   └── theme/     # NetflixColors, Dimens, Motion, Theme
│   │   │   └── MainActivity.kt# Root scaffold & Splash coordinator
│   │   └── res/
│   │       ├── drawable/      # Extracted Hawkins badges & WebP drawables
│   │       ├── mipmap-*/      # Official Netflix app icons across all densities
│   │       ├── raw/           # Ta-dum intro audio & Vorbis SFX
│   │       └── values/        # themes.xml (Theme.SplashScreen), colors.xml
│   └── build.gradle.kts       # App dependencies & Compose config
├── gradle/wrapper/            # Gradle 8.7 wrapper configuration
├── build.gradle.kts           # Root AGP 8.4.1 & Kotlin 1.9.23 plugins
└── settings.gradle.kts        # Repositories & project include
```

---

## ⚖️ Legal & Disclaimer

This project is created strictly for educational, research, and non-commercial reverse engineering demonstration purposes. All Netflix trademarks, logos, and visual assets belong to Netflix, Inc.
