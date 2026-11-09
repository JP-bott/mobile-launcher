# Liquid Launcher 4.0 Preview

A native Android home launcher, built with Kotlin and Jetpack Compose. Inspired by contemporary iOS home screen organization but using original styling and Android-native integrations.

## Features

**Home**
- Full-screen system wallpaper, with adjustable dark overlay
- Swipeable four-column home pages and compact date header
- Long-press icon edit mode, haptic feedback, spring lift, and position changes within or toward neighboring pages
- Four-app dock with saved shortcuts, page dots and swipe-down App Library shortcut
- Local persistence of home icons and dock ordering

**Folders**
- Create an app folder from the app shortcut options menu; choose a second app to combine
- Folder preview grid, rename, open apps and remove apps
- Folder layout is saved locally

**Apps and search**
- Launch installed Android applications
- Searchable App Library with Android-provided category filters
- Refresh installed apps when launcher resumes

**Customization**
- Adjustable icon size, labels, wallpaper darkness, optional monochrome icon effect, drag haptics
- Quick access to Android's default Home-app selection and wallpaper selection

**Android integrations**
- Widget picker and separate native widget-hosting panel, launched from Home edit mode → Widgets
- Optional notification badge counts. Enabling badges requires explicit Android notification access from the launcher settings.
- Simulated lock-screen preview with date, large clock, and swipe-up dismissal; **not a replacement for Android's secure lock screen**

## Build APK in GitHub

1. Go to [GitHub Actions](https://github.com/JP-bott/mobile-launcher/actions/workflows/build-apk.yml).
2. Open the newest workflow run with a green check.
3. Confirm that **Run launcher unit tests**, **Build debug APK**, and **Upload APK** succeeded.
4. Download the `LiquidLauncher-debug-apk` artifact from the run's **Artifacts** section.
5. Extract `app-debug.apk` and install on Android 8.0 or newer.

**Important:** GitHub-hosted debug builds may use different debug signing certificates in different runs. If Android reports that an update is incompatible, uninstall the previous debug version before installing the new one. This removes its saved layout/preferences. A stable release signing key should be set up privately before long-term use.

## Quality and security notes

- CI tests check the core pure-layout rules, then compile the APK. CI does **not** prove runtime correctness or 60 FPS across phones.
- Notification text is not stored or transmitted by this app; only in-memory counts per package are used, with user-controlled notification listener permission.
- The real system PIN, fingerprint, lock screen, status bar and Control Center remain controlled by Android.
- The app uses the installed apps' real icons by default. Optional monochrome tint is an approximation, not Apple's icon assets.
- No telemetry or account sign-in code is included.

## Known limitations

This is a preview build, not an exact iOS 26 replica or a production-ready launcher:
- Folders are created through shortcut options, **not** by dwelling over another icon during drag.
- Dragging between pages uses edge-based movement; it needs device testing.
- Android widgets currently render in their own **Widgets panel**, not as freely placeable/resizable home-grid tiles.
- Empty grid positions and custom page coordinates are not yet modeled. Pages use ordered app lists.
- Gesture navigation, blurred background refraction and OEM/device performance still require device-specific testing.
- The App Library categories use Android app metadata, which can be incomplete or inconsistent.

Keep your existing Android home launcher installed as a fallback. To restore it, visit Android Settings → Default apps → Home app.
