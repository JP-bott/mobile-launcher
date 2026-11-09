# Liquid Launcher

A Kotlin + Jetpack Compose Android home launcher with an **iOS 26-inspired** translucent interface. This is an independent Android app, not Apple iOS.

## Features

- Home screen displaying installed launchable apps in a four-column grid
- Search field to filter apps by name
- Tap app icons to launch Android applications
- Translucent bottom dock with up to four shortcuts
- **Long-press an app** to pin or remove it from the dock; selections are saved locally
- Refresh installed-app listings periodically
- Lock-screen-style preview with live time and date and a swipe-up gesture

The lock screen is **only a preview**. It does not replace the Android system lock screen or provide authentication.

## Build without Android Studio

1. Visit [GitHub Actions](https://github.com/JP-bott/mobile-launcher/actions/workflows/build-apk.yml).
2. Select the latest **Build Android APK** run, or choose **Run workflow**.
3. Wait for the run to finish with a green check.
4. Under **Artifacts**, download `LiquidLauncher-debug-apk`.
5. Extract the archive to obtain `app-debug.apk`.
6. Transfer and install the APK on an Android phone (Android 8 or newer). Android may ask you to allow installs from your browser or file manager.
7. Optionally select **Liquid Launcher** under **Settings → Apps → Default apps → Home app**.

Only install an APK if the workflow succeeded. Debug APKs are intended for personal testing, not public release.

## Limitations and planned improvements

The interface uses translucent colors and an original gradient wallpaper, not a genuine system-wide Liquid Glass renderer. More work is needed for icons and folder layout persistence, multiple home pages, widget hosting, wallpaper handling, work profiles, accessibility, performance, and device compatibility. The source has not yet been validated on a physical device.

## If the build fails

Open the latest GitHub Actions run and examine the first failed build step. This README does not imply that an APK has been built successfully.
