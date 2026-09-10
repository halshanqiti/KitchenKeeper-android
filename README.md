# Kitchenkeeper — native Android

Kitchenkeeper 2.0 uses native Android views and an on-device SQLite database. Its inventory, equipment, shopping list and recipe journal open directly in the app and work offline. It does not use a WebView or Custom Tabs for the application.

## Install and keep your earlier records

The new native package is `com.kitchenkeeper.android`; the original browser launcher used `com.kitchenkeeper.android.personal`. Both can be installed together.

1. Install the signed **Kitchenkeeper-native.apk** supplied with this project.
2. Open Kitchenkeeper. No browser or account sign-in is required for the native inventory.
3. To bring records from the earlier version, export **Settings → Export kitchen & recipes** in the earlier kitchen. In the native app choose **Settings → Import kitchen backup** and select that JSON file.
4. New records are merged. Existing matching native records are kept. The web export contains photo references, so those photos need to be added again. New native backups can include actual photos.

The native app stores data on this phone and does not automatically synchronise with the earlier hosted kitchen. Export a backup before uninstalling it or changing phones. Your API key is excluded from backups.

## Features

- Native Material screens, bottom navigation, grocery/equipment details, storage places, dates, low-stock levels and photos.
- Offline recipe journal with ingredients, method, source links, notes, ratings, favourite recipes and tried dates.
- Native shopping list and transactional put-away; dated or opened stock remains in separate batches.
- Google Code Scanner camera interface for UPC/EAN/GTIN, with manual input. The first scan may download a Google Play services module. Database lookups need internet and match barcodes exactly against Open Food Facts / UPCitemdb. Check package variants, dates and allergens yourself.
- On-device ML Kit OCR for Latin-script labels, producing editable drafts. Dates are not guessed. Optional AI vision reads more complex or multilingual labels, including Arabic, using your OpenAI API key.
- Optional source-linked cooking research with explicit inventory-sharing consent. AI is not guaranteed factual. It searches selected sources and rejects responses without usable citations. Source links open externally only when tapped.
- Native camera/gallery selection and document-picker backup import/export. No broad storage permission is requested.
- OpenAI API keys encrypted on the phone using Android Keystore AES-GCM. API calls send only the selected photo, question and optionally stocked item details; private notes and serial numbers are excluded. OpenAI API usage is billed separately from ChatGPT. Response storage is disabled in requests; provider policies still apply.

## Build and verify

GitHub Actions runs `lintRelease`, compiles the application and instrumented tests, then launches an API 36 (Android 16) emulator. Tests exercise actual native screens, offline persistence, recipe saving, activity recreation, grocery batches, backup validation, on-device OCR, Android Keystore encryption and citation/barcode gates. They do not contact paid AI services or test a physical camera.

The **Kitchenkeeper-Native-Android** artifact contains an **unsigned** release APK, build metadata, checksums, emulator screenshots and the Android signing utility. It must be signed before installation. The signed APK supplied separately is signed privately with a stable key. No signing key or user API key is committed to this public repository. Keep the private signing backup for future updates.

Build requirements: JDK 17, Gradle 8.13, Android SDK platform 36, Build Tools 35.0.0. Run `gradle lintRelease assembleRelease` for the unsigned release, or `gradle assembleDebug` for a development build. Android 8 / API 26 is the minimum supported version.

## References

- [Android application fundamentals](https://developer.android.com/guide/components/fundamentals)
- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner)
- [ML Kit text recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [OpenAI web search](https://developers.openai.com/api/docs/guides/tools-web-search)
- [Open Food Facts API](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/)
- [UPCitemdb API](https://devs.upcitemdb.com/)
