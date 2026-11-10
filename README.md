# Liquid Launcher 5.0 Preview

A native Android home-screen launcher built with Kotlin and Jetpack Compose. The visual style is inspired by modern iPhone home screens, but this is an independent Android application, not Apple iOS.

## What's new in 5.0

- **Position-based 4 × 5 home grid:** each app, folder and widget has a saved page, row and column, including empty spaces
- **Improved drag-and-drop:** long press and drag to move apps into empty cells, swap single-cell shortcuts, or create a folder by dropping on an app
- **Cross-page placement:** drag toward a horizontal page edge to move to another home page
- **Persistent layouts:** local grid position storage and migration of the previous ordered home shortcuts
- **Original iOS-inspired icons:** category-inspired icons for familiar built-in apps such as Phone, Messages, Camera, Calendar, Settings, Photos and Clock; unknown apps retain their Android icons
- **Icon theme option:** Settings → iOS-inspired app icons; enabled by default on new installs
- **Real home-screen Android widgets:** choose Widgets from edit mode, then use Android's widget picker; selected widgets occupy a fixed 2 × 2 home tile
- Existing App Library search and categories, notification badge permission, wallpaper support, persistent dock, lock preview, and appearance settings remain available
- **PositionEngine unit tests:** check empty slots, collisions, cross-page moves, swaps and folder creation

## Installing without Android Studio

1. Visit [Build Android APK](https://github.com/JP-bott/mobile-launcher/actions/workflows/build-apk.yml).
2. Open the latest successful workflow.
3. Confirm **Run launcher unit tests**, **Build debug APK**, and **Upload APK** all passed.
4. Download the `LiquidLauncher-debug-apk` artifact.
5. Extract `app-debug.apk` and install it on your Android device.

A GitHub-hosted debug build may have a different signing certificate from an older APK. If Android rejects an update, uninstall the older debug build and install the new one. Uninstalling erases locally stored layouts. Long-term releases should use a stable signing key secured outside the repository.

## Important limitations

- These are original icon designs and are not licensed copies of Apple's proprietary iOS assets. Not every Android application has an iOS-inspired icon.
- Home widget tiles are currently fixed at 2 × 2. Fully interactive resizing, freeform sizing, multiple grid-size presets and provider configuration workflows need additional development and OEM testing.
- Dropping an app on another app attempts folder creation, but dense-grid drag behavior and page-edge motion still need real-device testing.
- The lock screen is an in-app preview and does not replace Android's secure system lock screen.
- CI compilation and unit tests do not verify frame rate, battery efficiency, accessibility, animation polish, notification behavior or stability on all devices.
- This is still a preview, not a finished pixel-accurate iOS 26 replica.

For initial testing, keep your original launcher installed. To switch back, use Android Settings → Apps → Default apps → Home app.
