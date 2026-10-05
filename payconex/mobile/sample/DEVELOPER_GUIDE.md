# Adapting the BluePOS Go samples

These projects are starting points for human and agent developers. The inline KDoc/Swift documentation explains the contracts at each integration boundary. Read this guide alongside the [operation matrix](README.md#sdk-capabilities) and the README in the platform you are adapting.

The samples use the supplied vendor SDKs and hand work to the separate BluePOS Go app. They do not contain a payment processor, reader driver, mock payment gateway, durable order store or recovery coordinator. A working UI or accepted handoff is not evidence of a successful payment.

## Read the source in this order

Paths below are relative to each project. Android Kotlin files are under `app/src/main/java/com/bluefin/testaidlgo/`; iOS Swift files are under `BluePosGoSample/BluePosGoSample/`.

| Concern | Android entry point | iOS entry point | What to learn or customize |
| --- | --- | --- | --- |
| Credentials and environment | `app/build.gradle.kts`, `data/PaymentHelpers.kt` | `CredentialStore.swift`, `CheckoutModel.init` | Local input precedence, Basic encoding, validation and conversion to vendor credentials |
| Input rules | `data/AmountInput.kt` | `PaymentInput.swift` | Two-decimal input, exact cents, positive amount versus zero/blank tip |
| Screen and intent | `ui/MainScreen.kt`, `ui/events/MainScreenEvents.kt` | `CheckoutView.swift` | Shared sections, per-action validation, explicit transaction selection, developer controls |
| SDK request construction | `data/PaymentHelpers.kt`, `MainActivity.processAction` | Action methods in `CheckoutModel.swift` | Operation-specific arguments, credentials, IDs and amount types |
| Handoff and completion | `MainActivity.startThroughService`, `paymentCallback` | `CheckoutModel.swift`, `SceneDelegate.swift`, `AppDelegate.swift` | Remote dispatch, UI thread/actor, return routing and result interpretation |
| Application registration | `app/src/main/AndroidManifest.xml` | `Info.plist`, Xcode target settings | Vendor package visibility versus the merchant app's own callback scheme/identity |
| Appearance | `ui/theme/Theme.kt`, `MainActivity`, `BluefinHeader` | `BluefinTheme.swift`, `SceneDelegate.swift` | Persisted default-dark preference, component palettes, logo and system UI |

The comments distinguish behavior the samples implement from work a host app must add. Keep that distinction when modifying the code or writing agent instructions; do not assume a recovery mechanism or test double exists merely because its insertion point is documented.

## Follow one operation end to end

### Android: Sale

1. `MainScreen` parses the editable strings to integer cents and enables Sale only for a positive amount, valid non-negative tip and present configuration.
2. The button emits `ProcessClick(amount, tip)`. `MainActivity.processAction` calls `createPaymentRequest`, which reads `BuildConfig`, constructs the Basic token, and converts cents to SDK `Double` major units.
3. `startThroughService` checks its service proxy and remote initialization, applies the SDK debug flag, then calls `payment(request, paymentCallback)` over AIDL.
4. An accepted request causes `openBluePosGoPayment` to launch the SDK-defined activity with the same command and payload. The service Boolean indicates dispatch acceptance, not approval.
5. `paymentCallback.onPaymentResult` transfers state updates to the UI thread, retains the returned vendor transaction ID if present, and displays an SDK summary. A production order workflow must interpret and reconcile the outcome separately.

Authorization uses the same request with type `auth`; save uses type `save` and zero amount/tip. Refund and capture have their own command/request pair. The generic dispatcher uses casts, so a new command must have the correct request type in both construction and dispatch. The service calls are synchronous on the sample's UI thread; isolate potentially blocking IPC when designing a larger app.

### iOS: Sale

1. `CheckoutView` calls `takePayment(type: .sale)`. `CheckoutModel` revalidates its editable strings and constructs a `PaymentRequest` using Decimal major units, configured credentials, a fresh request UUID and the payment callback URL.
2. `begin` marks the local operation busy **before** dispatch. The SDK can complete immediately on a launch/validation failure; marking busy afterward could overwrite completion cleanup.
3. `BluePosGo.shared.startPayment` uses the default URL-scheme transport and opens BluePOS Go. `SceneDelegate` forwards warm-return and cold-launch URLs to `handleCallback`; `AppDelegate` also wires its URL entry point.
4. The SDK invokes the stored completion. `received` releases the local busy guard, then the action interprets success, decline, cancellation, failure or an unknown future result. Returning to the scene without a matched completion does not establish the outcome.

Keep a model/coordinator alive across the handoff. A new process cannot recover the old model's closures just because its launch URL was forwarded. If you change the merchant callback scheme, update both `CallbackPath.scheme` and `Info.plist`, plus the visible developer-panel hint.

## Contracts to preserve when adapting

### Amounts and transaction references

- The sample UI assumes USD and two fractional digits. Dot and comma are decimal separators; grouping, signs, exponents and extra precision are rejected. Blank tip means zero. Sale, authorization, capture, partial refund and clear-data read require a positive amount in this UI.
- Android UI events carry **cents as Long**. Only the request adapter converts to the SDK's **Double major units**. Exact parsing does not make all large Long values representable as Double; enforce the supported business/processor range.
- iOS uses **Decimal major units**, except capture, which requires an ungrouped **dot-decimal string**. Do not reuse a locale-dependent display formatter for that SDK field.
- A selected transaction ID is a vendor reference, not a local order ID or proof of refund/capture eligibility. Full refund ignores the amount field; partial refund and capture use it. Tip is used only for sale/authorization.
- iOS `ORDER-1001` and `CUSTOMER-1001` are fixed demonstration metadata. Replace them with meaningful host-app references. The request UUID correlates an SDK flow; it does not implement merchant idempotency or duplicate prevention.

### Configuration

Android configuration precedence is Gradle property, environment variable, then local `payment.properties`. iOS loads the bundled local `payment.plist`; an explicit Basic token overrides its API key/secret pair. Both projects provide blank examples, default a blank environment to STAGING, and require a rebuild after configuration edits. Use the platform README for the exact keys and setup.

Git exclusion protects source control, not the shipped binary: Android embeds values in generated `BuildConfig`, and iOS bundles the plist. Basic tokens are reversible Base64 credentials. Choose credential provisioning appropriate to your deployment; neither app supplies a production secret-distribution solution. Keep real values out of example files, comments, fixtures, build output and screenshots. Configuration checks establish local structure/presence, not authentication.

### Lifecycle and outcomes

Android binds in `onStart` and unbinds in `onStop`. Binding may be pending while its proxy is null; `serviceBound` tracks the need to unbind. A bound service is not proof of initialization or reader connection. Unbinding or encountering an IPC/launch error does not cancel a remote payment. The sample saves only the selected transaction ID and saveable input text; it does not durably track an operation through Activity recreation or process death.

On iOS, `isBusy` coordinates this one model's operations. It is not persisted remote state. `cancelPending` clears SDK completion tracking and releases this local guard; it does not stop the reader or reverse a transaction. Its calls to all cancellation methods assume that this sample owns every SDK flow. Scope cancellation to the owning flow in an app with multiple clients.

For a real checkout, add durable order-to-operation references, explicit pending/unknown/terminal states, reconciliation and a deliberate retry policy. Persist enough non-sensitive context before handoff to recover after a lost callback or process restart. Do not automatically retry a financial operation merely because the app returned, IPC failed or local callbacks were cleared. The samples' status strings and transaction lists are developer feedback, not an order ledger or fulfillment decision.

### Diagnostics and platform differences

Both traces hold at most 50 in-memory metadata events. Add chosen operation names, stages and outcome categories; never dump request/response objects or full callback URLs. Clear-data callbacks deliberately ignore card payloads. User-visible SDK status/error summaries also require review before forwarding to analytics or durable logs.

Android's SDK debug flag is a real remote service option, separate from the local trace. This iOS SDK has no equivalent flag. Reader connect/disconnect/forget calls are Android-only; on iOS they are managed in BluePOS Go. Android has no SDK cancel-pending API. Preserve these distinctions when keeping the shared layout consistent.

## Extend or replace a flow

1. Check the **bundled SDK version and interface**, then identify its request type, required credentials/amount units, result cases and callback/handoff contract. Do not infer platform APIs from the sibling sample.
2. On Android, add the UI event, factory if needed, action mapping, correctly typed service dispatch and result handling. On iOS, add the model action, callback path and typed completion handling. Acquire/release busy state in the same order as existing flows.
3. Keep presentation in `MainScreen`/`CheckoutView` and vendor-specific conversion in the adapter/model. Add validation at any new non-UI entry point as well as button-enabled feedback.
4. Keep raw payloads out of diagnostics. Explain any operation-specific assumptions beside its request or callback, and update the shared capability matrix when behavior changes.
5. Add focused tests for new parsing/business rules and injected-gateway behavior, then verify the real handoff on a physical device. Review cold return, interruptions and uncertain outcomes as part of the host application's recovery design.

For independent unit tests, introduce a small gateway interface/protocol around the vendor calls and inject it into the coordinator. A fake can then deliver accepted, declined, cancelled, failed and delayed completions without launching BluePOS Go. The existing projects do not yet implement that abstraction; their current host tests cover input/configuration only.

## Verify without a reader

From `android-sample-app`, using the configured Java/Android toolchain:

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

With an Android emulator/device connected, `:app:connectedDebugAndroidTest` also runs the existing application-context installation smoke check. It does not exercise payment handoff or reader behavior.

From `ios-sample-app`, run the Foundation-only checks without a simulator:

```sh
checks_dir=$(mktemp -d)
xcrun swiftc BluePosGoSample/BluePosGoSample/PaymentInput.swift \
  Tests/PaymentInputTests.swift -o "$checks_dir/input-tests"
"$checks_dir/input-tests"
xcrun swiftc BluePosGoSample/BluePosGoSample/CredentialStore.swift \
  Tests/CredentialStoreTests.swift -o "$checks_dir/config-tests"
"$checks_dir/config-tests"
```

Compile the iOS app for a simulator without launching one:

```sh
xcodebuild -project BluePosGoSample/BluePosGoSample.xcodeproj \
  -scheme BluePosGoSample -configuration Debug \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

These checks establish parsing/configuration behavior and build compatibility. They do not validate successful BluePOS Go handoff, reader connectivity or payment outcomes. For those, use the configured vendor app and compatible physical hardware, and test the operation's actual success, decline, cancellation and failure cases.
