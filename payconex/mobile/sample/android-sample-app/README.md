# BluePOS Go Android SDK sample

This developer sample shows how an external Android app uses the supplied BluePOS Go SDK through AIDL and an activity handoff. Read the [developer adaptation guide](../DEVELOPER_GUIDE.md) for the source map, request lifecycle, documented sample limits and extension points. The source comments explain the contracts beside their implementation; also consult the documentation supplied with the SDK.

## Local payment configuration

Payment credentials are intentionally not committed to Git. Create a local
`payment.properties` file in the project root:

```properties
BLUEPOS_ACCOUNT_ID=your-account-id
BLUEPOS_API_KEY=your-api-key
BLUEPOS_API_SECRET=your-api-secret
BLUEPOS_ENVIRONMENT=STAGING
```

You can also provide the same names as Gradle properties or environment
variables.

## Shared developer interface

See [the sample overview](../README.md) for the common layout, operation mapping and platform differences. Developer tools include a local request/callback trace and the SDK debug-mode flag. Transaction rows select an ID; full and partial refunds are explicit actions in Payments.

## Build and checks

Use JDK 21 (the Gradle daemon toolchain), Android SDK Platform 37.0 and Build Tools 36.0.0.
The project pins Gradle 9.8.0, Android Gradle Plugin 9.4.1, Kotlin/Compose compiler 2.4.20,
and Compose BOM 2026.09.00. The wrapper verifies its distribution against Gradle's SHA-256.
The minimum supported device API remains 26; compilation now matches the existing target API 37.

Build, check input parsing, and run lint across build variants:

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:lintRelease
```

The required local Java/Android toolchain is configured in Android Studio. An emulator can run the UI; reader operations require BluePOS Go and compatible hardware.

With an emulator/device connected, run `sh ./gradlew :app:connectedDebugAndroidTest`
for the installation smoke test. It does not perform a payment.
