# BluePOS Go iOS sample

Sample merchant app for the semi-integrated BluePOS Go flow on iOS 13 and later. It follows the BluePosGoSDK and operations guides: register a callback URL, forward every open URL to `BluePosGo.shared.handleCallback(url:)`, initialize, then start a payment or another operation with a fresh request ID.

The private Bluefin package is not in this repository. `BluePosGoSDK/` is a local stand-in with the same call surface, so the project builds before you receive the distribution package. It does not talk to a reader or to Bluefin. Replace it before certification or production testing.


## Requirements

- Xcode with Swift Package Manager support.
- iOS 13 or later. This is the package minimum, not a compatibility promise for every installed BluePOS Go build.
- BluePOS Go installed and provisioned on the physical test device. Pin the SDK version and the BluePOS Go build you tested, and verify that pair on a physical device.
- A unique URL scheme that BluePOS Go can use to reopen your app.

The package product is named `BluePosGoSDK` and the package links the system `z` library automatically.

The sample deployment target is iOS 13. URL return is handled in `SceneDelegate` and `AppDelegate`, not `onOpenURL`, because the SwiftUI `onOpenURL` modifier is iOS 14+.

## Setup

1. Open `BluePosGoSample/BluePosGoSample.xcodeproj`.
2. Select the `BluePosGoSample` target, then **Signing & Capabilities**, and choose your team.
3. Resolve the local package if Xcode prompts you. The project references `../BluePosGoSDK`.
4. Run on a simulator. Enter any non-empty Basic token and account ID, choose `STAGING`, then tap **Initialize** and **Sale**.

The stand-in does not launch BluePOS Go. After each call it waits briefly and feeds `handleCallback(url:)` a callback URL, which is the same path BluePOS Go uses to reopen your app. The **Demo return** picker selects approved, declined, cancelled, failed, or masked.

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

1. Remove the local package reference from the project.
2. Choose **File > Add Package Dependencies…** and enter the package location supplied by Bluefin. Add the `BluePosGoSDK` product to the application target.
3. BluePOS Go reopens the app and `SceneDelegate` forwards that URL.
4. Keep credentials in an authenticated backend or protected configuration. Do not commit a production Basic token or account ID.
5. Test on a physical device with BluePOS Go installed. Confirm success, decline, cancellation, missing-app, and cold-return.

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
BluePosGoSample/
  BluePosGoSDK/                          Bluefin Package
  BluePosGoSample/BluePosGoSample.xcodeproj
  BluePosGoSample/BluePosGoSample/       App sources and Info.plist
```
