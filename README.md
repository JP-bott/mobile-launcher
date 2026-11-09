# Liquid Launcher

A native Kotlin + Jetpack Compose Android launcher with an original, iOS 26-inspired translucent visual design.

## Current features
- Horizontal swipe navigation between home-screen pages and a separate searchable **App Library**
- Four-column app grid with app icons and labels
- App shortcuts that launch installed Android applications
- Clock/date card on the first home page
- Bottom glass-style dock with up to four shortcuts
- Long-press any app to add or remove it from Home and Dock, or change its order
- Local persistence for home layout and dock shortcuts
- App catalog refresh on activity resume
- In-app simulated lock screen with large clock and swipe-up gesture
- Atmospheric gradient wallpaper and translucent, softly outlined surfaces

**Limitations:** This is not actual Apple iOS or an Apple launcher. App icons are the installed Android apps' original icons. The glass look currently uses transparency, gradients, and outlines; it does not implement true iOS 26 refraction or backdrop blur. The lock screen is an in-app **preview only** and does not replace or bypass Android device security. Full drag-and-drop icon placement, folders, widget hosting, and system wallpaper integration are not yet implemented.

## Build APK without Android Studio
1. Visit [GitHub Actions](https://github.com/JP-bott/mobile-launcher/actions/workflows/build-apk.yml).
2. Open the newest **Build Android APK** run, or choose **Run workflow**.
3. Confirm **Build debug APK** and **Upload APK** succeeded.
4. Download the **LiquidLauncher-debug-apk** artifact and unzip it.
5. Transfer `app-debug.apk` to your Android phone and install it.

## Testing
- Start the app and verify installed applications appear.
- Tap an app to launch it and use Android Home to return to the launcher.
- Swipe horizontally to move between pages and reach App Library.
- Long-press an icon to customize Home or Dock; close and reopen to confirm settings persist.
- Open the Lock preview and swipe up to return.
- Set Liquid Launcher as default under Android Settings → Default apps → Home app only after confirming it works on your device.
- You can restore the original default launcher in Android Settings at any time.

This project is a prototype. GitHub Actions compilation confirms a build only, not correct behavior on all devices. Debug builds are intended for testing, not store release.
