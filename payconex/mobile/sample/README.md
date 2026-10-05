# BluePOS Go SDK samples

These are developer samples for exploring the supplied Bluefin SDKs, not production checkout apps. Both apps use the **BluePOS Go Sample** title and the same screen structure:

1. **Status** — latest result and Initialize.
2. **Payment details** — amount and tip in USD; transaction ID for post-processing.
3. **Payments** — Sale, Authorization, Capture, Save card, Full refund, Partial refund.
4. **Reader** — Reboot reader, Clear-data read, Connect, Disconnect, Forget reader.
5. **Transactions** — refresh the SDK list and select a transaction to populate its ID.
6. **Developer tools** — transport information, diagnostics, clear result, pending callback cancellation, SDK debug mode on Android.

Both start with amount `1.00`, tip `0.00`, and an empty transaction ID. Amount entry accepts a decimal point or decimal comma, with at most two decimal places. Invalid input disables the relevant actions. Capture and partial refund use Amount; full refund uses only the transaction ID. Selecting a transaction does not automatically send a refund or capture. Eligibility is decided by BluePOS Go/the processor, not inferred from a local balance calculation.

## SDK capabilities

The layout includes the same core controls, but does not invent SDK calls that are absent from a platform. Unavailable reader/operation controls are disabled with explanatory text. The Android-only SDK debug-mode switch is omitted from iOS; both platforms retain the useful sample diagnostics.

| Function | Android SDK 1.1.0 | iOS SDK 1.0.0-dev.7.feb5e9a8 |
| --- | --- | --- |
| Initialize | `init` | `initialize` |
| Sale / authorization | `payment` (`sale` / `auth`) | `startPayment` |
| Save card | `payment` (`save`) | `save` |
| Full refund | `fullRefund` | `fullRefund` |
| Partial refund | `refund` | `partialRefund` |
| Capture | `capture` | `capture` |
| Clear-data read | `clearDataRead` | `clearData` |
| Reboot reader | `reboot` | `reboot` |
| List transactions | `getTransactionsListResponse` | `getTransactionList` |
| Connect / disconnect / forget reader | Direct SDK commands | Manage in BluePOS Go; no SDK methods |
| Cancel pending callbacks | No SDK method; cancel in BluePOS Go | `cancelPending…()`; clears local callbacks, does not reverse payment |
| SDK debug-mode flag | `setDebugMode` | No equivalent flag |
| Sample diagnostics | Request and callback trace | Request and callback trace |

## Developer diagnostics

**Show diagnostics** is enabled initially. It controls an in-memory trace (latest 50 events) of request dispatch, callback completion, and connection/failure stages. **Clear diagnostics** resets it. The sample does not log raw request/response objects, credential values, or clear card data. This is an integration trace, not a mock outcome generator. Android additionally exposes the real SDK debug-mode switch, initially off; it is applied to the service when a request is sent. iOS has its own pending-callback cancellation control with its narrower behavior explained in the UI.

The samples use real SDK integrations: Android AIDL/activity handoff and iOS URL-scheme/callback handoff. Virtual devices can demonstrate the sample UI and missing-service/app behavior. Successful reader operations need BluePOS Go and compatible physical hardware. The obsolete iOS Demo return picker has been removed.

## Local configuration

- Android: copy `android-sample-app/payment.example.properties` to `payment.properties` in that project.
- iOS: copy `ios-sample-app/payment.example.plist` to `payment.plist` in that project.

Actual configuration files are Git-ignored. The screen shows the environment only; credentials remain outside the UI. Rebuild after configuration changes. See each project README for build instructions.

## Relevant source

Start with the [developer adaptation guide](DEVELOPER_GUIDE.md) for a source-reading order, end-to-end request walkthroughs, contracts to preserve, extension points and command-line checks. The Kotlin and Swift source includes inline documentation at the integration boundaries and each SDK operation.

- Android: `ui/MainScreen.kt` (screen and events), `MainActivity.kt` (AIDL integration and diagnostics), `data/PaymentHelpers.kt` (SDK requests), `data/AmountInput.kt` (exact input parsing).
- iOS: `CheckoutView.swift` (screen), `CheckoutModel.swift` (SDK requests, results and diagnostics), `PaymentInput.swift` (exact input parsing).

The amount parsers have host-side boundary tests. Build and run both apps to verify the shared layout; hardware payment results still require device testing.

## Bluefin branding

The native samples share the Bluefin logo, navy/blue/yellow palette, diagonal header, and light/dark component styling. See [BRANDING.md](BRANDING.md) for asset sources, color values, and theme entry points. Payment actions and developer diagnostics use the same shared layout.

## Appearance preference

Use **Dark mode** in the branded header to switch between light and dark. Both apps default to dark, remember your choice across launches, and apply it independently of the phone’s system appearance. The iOS developer panel shows request/callback diagnostics and pending-callback cancellation, without an unsupported debug-mode switch.
