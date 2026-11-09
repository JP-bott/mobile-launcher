# Liquid Launcher • Premium Home UI (prototype)

A native Android home launcher built with **Kotlin + Jetpack Compose**, drawing inspiration from the layout and visual behavior of iOS home screens. This is not Apple iOS or an Apple product.

## Current interface
- System wallpaper behind the home screen, using the Android wallpaper window feature
- Four-column home screen with a compact date header
- Up to five rows per page and horizontal home-screen swiping
- Centered page indicator, floating Search button and fixed four-icon bottom dock
- Long-press a home-screen icon to enter edit mode; drag within a page to reposition
- Spring-scale lift animation and long-press haptic feedback when dragging
- Red minus controls in edit mode to customize shortcuts
- App Library overlay with searchable list of all launchable apps
- Saved home icon order and dock shortcuts
- Home app registration and installed-app refresh when launcher resumes
- Simulated lock-screen preview with date, large clock and swipe-up dismissal (not device authentication)

## Build without Android Studio
1. Visit [Build Android APK](https://github.com/JP-bott/mobile-launcher/actions/workflows/build-apk.yml).
2. Open the newest successful workflow run.
3. Scroll down to **Artifacts** and download `LiquidLauncher-debug-apk`.
4. Extract the ZIP and install `app-debug.apk` on Android 8.0+.

## Important limitations
- The project is a **working prototype**, not a finished pixel-accurate replica of iOS 26.
- No Apple proprietary icons, system assets, Control Center or real lock-screen replacement.
- Dragging is supported **within one home page**. Cross-page dragging, folders, true Android widget hosting, icon themes and complete home-page editing are not yet implemented.
- System wallpaper appearance may vary with OEM Android wallpaper and navigation settings.
- A successful CI build verifies compilation, but **does not verify runtime behavior, FPS, battery life, visual quality or compatibility with your device**.

## Device test checklist
1. Open launcher and confirm the system wallpaper is visible.
2. Launch an application and press Home to return.
3. Long-press an icon, drag it to another grid position and tap Done.
4. Restart the launcher to verify icon order and dock shortcuts remain saved.
5. Swipe between pages and open Search/App Library.
6. Enter and exit the simulated lock preview.
7. Restore your original Home app in Android Settings if needed.

For stability testing, keep the original launcher installed and available until this one has been tested on your phone.
