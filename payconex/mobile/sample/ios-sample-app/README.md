# BluePOS Go iOS sample

Sample merchant app for the semi-integrated BluePOS Go flow on iOS 13 and later. It follows the BluePosGoSDK and operations guides: register a callback URL, forward every open URL to `BluePosGo.shared.handleCallback(url:)`, initialize, then start a payment or another operation with a fresh request ID.

This prepared copy includes the real Bluefin SDK supplied in the SendSafely download: `BluePosGoSDK/BluePosGoSDK.xcframework` (1.0.0-dev.7.feb5e9a8), referenced through a local Swift package. Keep `BluePosGoSDK` and `BluePosGoSample` beside each other. The app source includes compatibility fixes for this SDK.

Read the [developer adaptation guide](../DEVELOPER_GUIDE.md) for the source map, request lifecycle, documented sample limits and extension points. The source comments explain request construction, callback routing, input rules and configuration beside their implementation.

## Requirements

- Xcode 26.5 with the iOS platform component installed (the local package uses Swift tools 6.3)
- A physical iPhone for the BluePOS Go handoff and reader tests
- A signing team set on the `BluePosGoSample` target
- BluePOS Go installed and provisioned on the iPhone, with the reader working in that app

The sample deployment target is iOS 13. URL return is handled in `SceneDelegate` and `AppDelegate`, not `onOpenURL`, because the SwiftUI `onOpenURL` modifier is iOS 14+.

## Setup

1. Copy `payment.example.plist` to `payment.plist` in this directory and fill in the local configuration described below.
2. Open `BluePosGoSample/BluePosGoSample.xcodeproj`.
3. Select the `BluePosGoSample` target, then **Signing & Capabilities**, and choose your team.
4. Resolve the local package if Xcode prompts you. The project references `../BluePosGoSDK`.
5. Connect the iPhone, select it as the run destination, and build/run. Complete any device trust and Developer Mode prompts.
6. Initialize before starting a test payment. Configuration is read from the bundled file; there are no credential-entry fields in the app.

The real SDK hands operations to BluePOS Go, which communicates with the reader and returns to this app. The sample uses the shared developer interface described in [the sample overview](../README.md), including selectable transactions and local request/callback diagnostics. There is no simulated-outcome picker.

## Local payment configuration

`payment.plist` is a local, Git-ignored file. Edit its string values in Xcode's property-list editor. Commit only the blank `payment.example.plist` template.

| Key | Value |
| --- | --- |
| `BLUEPOS_ACCOUNT_ID` | Your PayConex account ID |
| `BLUEPOS_API_KEY` | Your API key ID, matching Android's configuration |
| `BLUEPOS_API_SECRET` | Your API secret, matching Android's configuration |
| `BLUEPOS_ENVIRONMENT` | `STAGING`, `CERT` or `PROD`; blank defaults to `STAGING` |
| `BLUEPOS_BASIC_TOKEN` | Optional existing Basic token; overrides the API key/secret pair |

With an API key and secret, the app constructs `Basic Base64(apiKey:apiSecret)` automatically. If you already have a Basic token, fill in `BLUEPOS_BASIC_TOKEN` and leave the API key/secret blank. The token may include the `Basic ` prefix or contain only the encoded credential. Account ID and environment are still required/configured separately. These are API credentials, not a website username and password.

The same file is copied into Debug and Release builds. Rebuild and reinstall after changing it. Missing values or an invalid environment are shown in the Status section and disable operations. A fresh checkout needs the local file created before Xcode can copy it into the app.

Git ignores the source configuration file; the built app still contains these credentials. Use this build-time configuration for the intended sample/test deployment.

The Android sibling uses its own Git-ignored `../android-sample-app/payment.properties` file with the same account, API key, secret and environment names. The two apps do not read one another's local files.

## Configuration checks without a simulator

Run from this directory. The checks use synthetic credentials only:

```sh
config_check_dir=$(mktemp -d)
xcrun swiftc BluePosGoSample/BluePosGoSample/CredentialStore.swift \
  Tests/CredentialStoreTests.swift -o "$config_check_dir/config-tests"
"$config_check_dir/config-tests"
```

## URL scheme

The app registers `mymerchantapp` in `Info.plist`. Every request uses that scheme:

- `mymerchantapp://bluepos/initialize`
- `mymerchantapp://bluepos/payment`
- `mymerchantapp://bluepos/save`
- `mymerchantapp://bluepos/transactions`
- `mymerchantapp://bluepos/refund`
- `mymerchantapp://bluepos/capture`
- `mymerchantapp://bluepos/clear-data`
- `mymerchantapp://bluepos/reboot`

Change the scheme to one that belongs to your organization, in both `Info.plist` and `CallbackPath`. The SDK accepts a callback only when scheme, host, and path match the active request.

## Use SDK

The real SDK is already connected through `../BluePosGoSDK`; no remote package download or replacement is needed for this copy. BluePOS Go reopens the app and `SceneDelegate` forwards that URL.

Keep credentials in an authenticated backend or protected configuration. Do not commit a production Basic token or account ID. Test on a physical device with BluePOS Go installed. Confirm success, decline, cancellation, missing-app, and cold-return.

Leave the default `startPayment` transport (`.urlScheme`) unless you have tested `.gzip`, `.pasteboard`, or `.automatic` against the BluePOS Go version you ship.

## What the sample covers

| Action | SDK call |
| --- | --- |
| Initialize | `initialize(request:)` |
| Sale and authorization | `startPayment(request:)` |
| Save a card | `save(request:)` |
| Transaction list | `getTransactionList(request:)` |
| Full and partial refund | `fullRefund` / `partialRefund` |
| Capture | `capture(request:)` |
| Clear-data read | `clearData(request:)` |
| Reboot reader | `reboot(request:)` |
| Abandon a flow | `cancelPending…()` |

Refunds need `PaymentCredentials`. A save is a zero-amount card-on-file request. Capture sends the amount as a dot-decimal string, independent of device locale. The clear-data result is not shown or logged.

## Production checklist

- Generate an unguessable request ID per operation and match it to the order on your backend.
- Do not start an operation while `is…InProgress` is true. Cancel only the flow your screen owns.
- Persist payment and refund outcomes only after server-side reconciliation. The client callback is not the source of truth.
- Validate refund eligibility and the remaining refundable amount on the server.
- Keep Basic tokens, account IDs, PAN, and Track 2 out of source control, analytics, and logs. A clear-data read may create PCI obligations.

## Layout

```
ios-sample-app/
  payment.example.plist                Blank configuration template
  payment.plist                        Local configuration (ignored by Git)
  Tests/CredentialStoreTests.swift      Host-side configuration checks
  BluePosGoSDK/                          Local package and real Bluefin XCFramework
  BluePosGoSample/BluePosGoSample.xcodeproj
  BluePosGoSample/BluePosGoSample/       App sources and Info.plist
```

## Verified build

The shared-interface revision builds for the iOS Simulator with Xcode 26.5 and was run on the iPhone 17 simulator. The previous device build was installed on an iPhone; this revision still needs physical reader transaction testing. Simulator checks do not validate a payment or reader connection.

```sh
xcodebuild -project BluePosGoSample/BluePosGoSample.xcodeproj \
  -scheme BluePosGoSample -configuration Debug \
  -destination "generic/platform=iOS" CODE_SIGNING_ALLOWED=NO build
```

## Shared developer interface

See [the sample overview](../README.md) for the common screen structure, SDK capability differences, and debugging controls. Run payment-input checks with:

```sh
input_check_dir=$(mktemp -d)
xcrun swiftc BluePosGoSample/BluePosGoSample/PaymentInput.swift \
  Tests/PaymentInputTests.swift -o "$input_check_dir/input-tests"
"$input_check_dir/input-tests"
```
