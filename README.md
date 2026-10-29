# Kalp ❤️

Minimal Android couples counter starter project.

## Build APK with GitHub
1. Create a new public or private GitHub repository named **Kalp**.
2. Upload the contents of this project to the repository root (not the ZIP folder itself).
3. Open **Actions** → **Android APK** → **Run workflow** (or push to `main`).
4. Download `Kalp-debug-apk` from the completed workflow run's Artifacts section.

## Current first version
- Minimal dark interface with a heart mark.
- Choose relationship start date.
- Enable/disable counter.
- Android home-screen widget showing elapsed days.

## Important: live distance and lock-screen support
The project currently contains the visual/app/widget foundation. Live distance between two phones is **not active yet**: it needs a secure pairing service (for example Firebase Authentication + Realtime Database), two users to pair, and location permissions on both devices. Do not publish a database with public read/write rules.

Android home-screen widgets are supported. Lock-screen widgets are not consistently available on Android phones; availability depends on Android version and manufacturer. The same widget can be placed on the home screen on supported launchers.

## Next implementation step
Configure Firebase for package `com.kalp.app`, add `google-services.json`, then implement authenticated pairing and foreground location sharing with an explicit on/off control. Both users must consent to location sharing.
