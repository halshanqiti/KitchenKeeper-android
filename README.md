# Kitchenkeeper for Android

Kitchenkeeper opens your private digital kitchen from an Android launcher icon, using your browser’s sign-in session. It includes the hosted grocery and equipment inventory, barcode lookup, editable label scans, shopping list, recipe journal and cooking assistant.

**The Android shell opens the hosted application using Custom Tabs. It requires a supporting browser and internet access.** It is not an offline native rewrite. Sign in with the ChatGPT account that owns the kitchen. The repository contains app code; your inventory, photos and API key are not stored here.

## Install on your phone

1. Download the APK from a successful [Android build](https://github.com/halshanqiti/KitchenKeeper-android/actions/workflows/android.yml). The downloadable artifact is named **Kitchenkeeper-Android-APK**; extract it if it downloads as a ZIP.
2. Open **Kitchenkeeper-personal.apk** on your Samsung phone.
3. If Android asks, allow that browser or file manager to install this app, then confirm **Install**. Installation still depends on your phone’s security and organisation settings.
4. Open **Kitchenkeeper** and sign in with the same ChatGPT account if asked.

This is a development-signed personal build, not a Play Store release. The workflow checks Android lint, compiles the APK and verifies its signature. A successful build does not mean the app has been tested on your specific phone.

The launch screen provides **Open my kitchen** if you return from the browser. Barcode scanning and photo permissions are handled by the browser. Browser controls may be visible. Chrome and Samsung Internet are suitable browser choices; live barcode detection depends on browser support, and manual barcode entry is always available.

## Your kitchen

- **Food / Tools:** groceries, equipment, quantities, storage places, dates, photos and product details.
- **Scan & add:** exact barcode lookups from Open Food Facts / UPCitemdb, or a label photo that becomes an editable draft. Check the packaging before saving. Barcode databases do not supply a batch’s expiry date.
- **Recipes:** save ingredients, your method, a source link, photos, favourites, ratings and notes about recipes you have tried.
- **Shop:** tick your purchases, then put them away into inventory. Dated stock stays in separate batches.
- **Cooking assistant:** source-linked recipe ideas, equipment guidance and cooking research. AI can be incorrect; inspect the linked evidence, especially for safety or your exact appliance.

For label reading and AI answers, open **Settings → AI connection** in the private app and enter an OpenAI API key. API usage is billed separately from ChatGPT. The key is encrypted when saved and is never included in exports or this Android package. Label reading sends the selected photo; the assistant sends your question and optionally stocked kitchen details. Do not commit API keys or paste them into a chat.

Hosted app changes appear automatically without rebuilding the Android shell. Date alerts appear inside the app; background phone notifications are not included.

## Build with GitHub Actions

A push that changes Android source or its workflow builds the personal APK. You can also open **Actions → Build Kitchenkeeper Android APK → Run workflow**.

The artifact contains the APK, these instructions, SHA256SUMS.txt, a signature-verification report and build metadata. Artifacts are retained for 30 days. Check that the workflow succeeded before downloading.

Fresh cloud builds normally generate a new debug signing key. A later shell build may therefore require uninstalling the old shell first. Your hosted inventory remains in your account. For stable distributed updates, configure your own release signing key and keep it private.

## Build locally with the Android SDK

Requirements: JDK 17, Gradle 8.13, Android SDK platform 36 and Build Tools 35.0.0. Google Maven, Maven Central and the Gradle distribution service must be reachable.

Set `ANDROID_HOME` or an untracked `local.properties` file containing `sdk.dir`. Run:

```sh
sdkmanager 'platforms;android-36' 'build-tools;35.0.0'
gradle --no-daemon lintDebug assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. `build-local.sh` checks the local requirements before building.

Gradle wrapper metadata is included. If wrapper launchers and the wrapper binary are absent, generate them after installing Gradle:

```sh
gradle wrapper --gradle-version 8.13
```

Then open the folder in Android Studio, use JDK 17 as its Gradle runtime, and let it sync. GitHub Actions installs Gradle directly and does not require wrapper files.

## Configuration

- Namespace: `com.kitchenkeeper.android`
- Personal debug package: `com.kitchenkeeper.android.personal`
- Android API: minimum 26; compile / target 36
- Android Gradle Plugin: 8.13.2
- AndroidX Browser: 1.10.0
- Hosted app URL: `app/src/main/res/values/strings.xml`, `kitchen_url`

## References

- [Android Custom Tabs](https://developer.android.com/develop/ui/views/layout/webapps/overview-of-android-custom-tabs)
- [AndroidX Browser](https://developer.android.com/jetpack/androidx/releases/browser)
- [Android Gradle Plugin 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Building APKs](https://developer.android.com/build/building-cmdline)
- [APK signature verification](https://developer.android.com/tools/apksigner)
