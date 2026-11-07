# IndoorOutdoorDetection — Android multimodal label logger

This is the Android Studio/Java source project provided by the authors for the OpenMobileContextEnvSense accompanying software release. It records mobile sensor, GNSS, cellular, Wi-Fi, and audio summary measurements alongside environment, transport, motion, and device pose annotations.

## Build

Open `android/IndoorOutdoorDetection/` in Android Studio, allow Gradle to sync, and run the `app` configuration on a supported Android device. The supplied project specifies Android Gradle Plugin 7.3.1 and Gradle wrapper 7.4, compile SDK 34, minimum SDK 21 and Java 11 compatibility. Build with a compatible Android Studio/JDK combination; a clean-room rebuild has **not** been verified in this release workspace.

Permissions requested by the application include fine/coarse/background location, phone state, microphone, Wi-Fi, and broad storage. The user should grant only permissions necessary for approved collection protocols. Android platform permission policies and background recording constraints may limit functionality on recent phones. The source is distributed for research inspection, not as a production-ready privacy-hardened consumer application.

## Privacy

The logger can capture personally or geographically sensitive raw information. Do not publicly share unprocessed CSV exports. The accompanying `data/datasets/public_2026_11_07/` material is a separately processed, de-identified six-session example. The raw original session recordings are **not** included here. Refer to that folder's README for the limits of de-identification and supported uses.

## License

Source code follows the repository's MIT license unless third-party component licensing states otherwise. No Android SDK, Android Studio cache, signing keys, device-specific SDK paths, or compiled application binaries are included. Third-party dependencies are declared in Gradle and remain governed by their respective licenses.
