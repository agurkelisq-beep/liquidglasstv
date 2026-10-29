# Glass TV — Liquid Glass-inspired Android TV launcher (v0.1)

A custom, Apple tvOS-inspired **Android TV app** that opens installed applications with your TV remote. It uses original artwork and native Android views (no copyrighted Apple images or icons). Built for testing on **Telia TV 245 / DVB919X-LT, Android TV 14**; this device has **not yet been physically tested**.

## What's here

- Frosted, semi-transparent dock and controls with glossy focus outlines
- Three original cinematic backgrounds; switch with the Wallpaper button
- Android TV D-pad focus, OK to launch, Back / Exit to leave
- Discovers Android TV apps through launch intents; can also discover some standard Android apps
- Long-press OK on an app to pin/unpin it (MENU button may work as an alternative)
- Live clock, date, app count, responsive horizontal-scrolling app shelf
- Works as a **normal standalone TV app**, so your Telia home screen remains untouched
- Phone-only build workflow through GitHub Actions, using AGP 8.7.3 and Gradle 8.9

**Visual limitation:** This is an original glass *approximation*: translucent gradients, focus animations and bloom, **not Apple's proprietary Liquid Glass rendering or genuine live background refraction/blur**. Some streaming apps and operator apps might not appear or launch because they do not expose standard Android TV launch intents, or are blocked by operator policy.

## Build using only your Android phone

There is no compiled APK in this ZIP. The GitHub Actions workflow will compile it in the cloud and provide a **debug APK**, if the build passes.

1. On your phone, download and extract `LiquidGlassTV-project.zip` to a folder. Check that `.github/workflows/build-apk.yml` is present (some file managers hide dot-folders).
2. On [GitHub](https://github.com/new), create a repository named `LiquidGlassTV`. GitHub's site in Desktop mode may be easier on mobile.
3. Get the extracted project files into the repository **with their full folder structure preserved**, including `.github/workflows/build-apk.yml`. One phone-only approach is GitHub **Codespaces** (if your account has access): open your repository → Code → Codespaces → Create codespace. Upload the ZIP using the Codespaces file explorer, then run the commands below in the Codespaces terminal. This moves hidden `.github` files too:
   ```bash
   unzip -q LiquidGlassTV-project.zip
   cp -a LiquidGlassTV/. .
   rm -rf LiquidGlassTV LiquidGlassTV-project.zip
   git add .
   git commit -m "Add Glass TV launcher"
   git push
   ```

   Alternatively, you can use a mobile Git client that preserves the project folders.

4. Open the repository's **Actions** tab. Select **Build Android TV APK**, then **Run workflow**. A push to `main` or `master` also starts a build.
5. After a successful build, open that workflow run and download the **Glass-TV-v0.1-debug-APK** artifact. This is usually a ZIP containing `app-debug.apk` — extract it on your phone.
6. Move the APK to the Telia box using a file-transfer app or an approved file-sharing method. Android TV must permit installation from that source; otherwise ask the device provider. Do not disable the Telia software.
7. Install APK → launch **Glass TV** from the TV apps screen. Press OK to open an app, Back to leave it, or choose Exit.

**If you use Codespaces:** It may have account quotas. `unzip` and Git may already be installed; if you can't use Codespaces, the same folder structure can be uploaded using a Git-capable Android app.

### Useful build diagnostics

- Workflow red? Open the `Compile debug APK` step and copy the error message for troubleshooting.
- App missing? Telia may restrict third-party app sideloading. We do not attempt to bypass operator restrictions.
- Installed apps missing? Some packages don't advertise launchable activities to third-party apps.
- UI looks flatter than Apple TV? Expected: v0.1 is optimized to avoid expensive real-time GPU blur.
- Home button returns to Telia? **Expected and intentional.** This app does not register itself as `HOME` and does not replace the system launcher.

## Project structure

- `app/src/main/java/com/aura/glasstv/MainActivity.kt` — launcher app list + focus/navigation
- `app/src/main/java/com/aura/glasstv/ScenicBackdrop.kt` — original cinematic scene art
- `app/src/main/AndroidManifest.xml` — TV app manifest
- `.github/workflows/build-apk.yml` — automatic debug APK build

### Safety / privacy

No root, no ADB modifications, no dangerous firmware changes, no network permission, no data collection. The app reads launchable installed-app entries and saves local wallpaper/pin preferences.

### Build stack

- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, compile SDK 35, min SDK 26, target SDK 35.
- GitHub Actions downloads Gradle 8.9 (the ZIP intentionally has no wrapper binary).
- JDK 17; no Android Studio needed on the phone.
