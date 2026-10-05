import BluePosGoSDK
import Foundation

/// iOS integration coordinator: CheckoutView edits fields/calls actions; this model validates,
/// builds vendor requests and displays their typed completions. BluePOS Go owns the reader UI;
/// SceneDelegate forwards its return URLs to the SDK so the stored completions can run.
///
/// The vendor singleton is MainActor-isolated, so checkout state and SDK calls live here too.
/// SceneDelegate owns one model for the scene lifetime; SDK closures capture it weakly to avoid
/// retaining a dismissed checkout. A lost model/process loses this sample's in-memory state.
/// When adapting, add durable order/operation storage and outcome recovery outside the view.
/// A protocol around the SDK calls is the seam for host-side mocks; none is implemented here.
@MainActor
final class CheckoutModel: ObservableObject {
    // Editable strings preserve partially typed/invalid input. Convert to Decimal only after
    // PaymentInput validates it; never use floating-point UI values to construct an amount.
    @Published var amount = "1.00"
    @Published var tip = "0.00"
    @Published var transactionId = ""
    @Published var statusText: String
    @Published var transactions: [BluePosGoTransaction] = []
    @Published var diagnosticsEnabled = true
    @Published var diagnostics: [String] = []
    // One local operation at a time. This flag is not remote reader/session state, and resets
    // with the process. Multiple SDK clients/scenes would need a shared operation coordinator.
    @Published private(set) var isBusy = false

    private let configuration: CredentialStore?

    var isConfigured: Bool { configuration != nil }
    var environment: String { configuration?.environment ?? "NOT CONFIGURED" }
    // Configuration presence is not proof of authentication or successful initialization.
    // Initialize first; this sample relies on SDK/BluePOS Go results for session/reader readiness.
    var canStart: Bool { isConfigured && !isBusy }
    var hasValidAmount: Bool { PaymentInput.amount(amount) != nil }
    var hasValidTip: Bool { PaymentInput.tip(tip) != nil }
    var hasTransactionId: Bool { !transactionId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    /// Bounded, in-memory metadata trace, independent of any vendor logger. Only pass chosen
    /// operation names, request IDs and outcome categories. Never stringify whole SDK requests,
    /// responses, credentials or callback URLs: these may include tokens or card data.
    private func trace(_ event: String) {
        guard diagnosticsEnabled else { return }
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        diagnostics = Array((diagnostics + ["\(formatter.string(from: Date()))  \(event)"]).suffix(50))
    }

    /// Acquire the local UI guard BEFORE calling the SDK: launch/validation failure may invoke
    /// completion immediately. Setting isBusy after dispatch could overwrite received() and
    /// leave the UI stuck. Every completion path, including errors, must release this guard.
    private func begin(_ operation: String) -> Bool {
        guard canStart else { return false }
        isBusy = true
        trace("Request: \(operation); URL-scheme handoff")
        return true
    }

    // A completion means this local wait ended, not necessarily that a transaction succeeded.
    // Each action below interprets its result enum; never turn this helper into "mark order paid".
    private func received(_ operation: String) {
        isBusy = false
        trace("\(operation) completion received; payload omitted")
    }

    // Clear only the displayed summary; this does not clear/cancel vendor transactions.
    func clearResult() { statusText = "No result yet." }

    // Presence check only. A selected/manual vendor ID is not proof of ownership, capture or
    // refund eligibility; those checks belong in the merchant workflow and processor response.
    private func requireTransactionId() -> String? {
        let id = transactionId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else {
            statusText = "Enter or select a transaction ID."
            return nil
        }
        return id
    }

    /// Read the build-bundled payment.plist once. Injecting Bundle supports alternate test
    /// resources; CredentialStore itself is Foundation-only and tested without this SDK.
    /// Missing/invalid configuration disables actions and exposes key names, never key values.
    init(bundle: Bundle = .main) {
        do {
            configuration = try CredentialStore.load(from: bundle.url(forResource: "payment", withExtension: "plist"))
            statusText = "Ready. Initialize before taking a payment."
        } catch {
            configuration = nil
            statusText = error.localizedDescription
        }
    }

    // Centralize conversion to the vendor credential type. Keep configuration separate from
    // text fields and diagnostics; the local plist is sample provisioning, not a secret vault.
    private func configuredCredentials() -> PaymentCredentials? {
        guard let configuration else { return nil }
        return PaymentCredentials(
            basicToken: configuration.basicToken,
            accountId: configuration.accountId,
            environment: configuration.environment
        )
    }

    /// Initialize BluePOS Go with account, API Basic token and environment before other flows.
    /// Every operation gets a fresh correlation UUID and a registered callback URL. A request
    /// UUID is not a merchant order ID or a promise of processor-side idempotency.
    func initialize() {
        guard let credentials = configuredCredentials() else { return }
        let request = InitializationRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("initialize"),
            basicToken: credentials.basicToken,
            accountId: credentials.accountId,
            environment: credentials.environment
        )
        guard begin("Initialization") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Initializing…"
        // Standard action pattern below: validate -> build -> begin -> dispatch -> received ->
        // interpret result. Keep @unknown default branches for future vendor enum additions.
        BluePosGo.shared.initialize(request: request) { [weak self] result in
            self?.received("Initialization")
            switch result {
            case .initialized(let response):
                self?.trace("Outcome: initialized")
                self?.statusText = "Initialized: \(response.status ?? "unknown")"
            case .failure(let error, let response):
                self?.trace("Outcome: failure")
                self?.statusText = "Initialization failed: \(error); \(response?.message ?? "")"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Sale and authorization share the SDK payment endpoint; CheckoutView selects .sale/.auth.
    /// Revalidate here as well as disabling UI buttons so other callers cannot bypass parsing.
    /// Amount/tip are Decimal major units (USD); other currencies need a reviewed scale/contract.
    func takePayment(type: PaymentTransactionType) {
        guard let credentials = configuredCredentials() else { return }
        guard let amount = PaymentInput.amount(amount) else {
            statusText = "Enter a valid amount."
            return
        }
        guard let parsedTip = PaymentInput.tip(tip) else {
            statusText = "Enter a valid non-negative tip with at most two decimal places."
            return
        }
        let request = PaymentRequest(
            requestId: UUID().uuidString,
            amount: amount,
            tip: parsedTip,
            transactionType: type,
            currency: "USD",
            callbackURL: CallbackPath.url("payment"),
            // Demonstration metadata, reused on every sample request. Replace with a real order
            // reference and persist its relationship to requestId/transactionId in your own app.
            customId: "ORDER-1001",
            notes: "In-store sale",
            credentials: credentials,
            requiresReaderReady: true
        )
        guard begin("Payment") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Payment started. Waiting for BluePOS Go…"
        // Uses the SDK's default URL-scheme transport. Dispatch intentionally opens BluePOS Go;
        // returning to this app is insufficient without handleCallback forwarding the result.
        BluePosGo.shared.startPayment(request: request) { [weak self] result in
            self?.received("Payment")
            switch result {
            // These are separate SDK outcomes. Keep decline/cancel/error distinct and retain
            // operation type: an authorization success is not a captured sale. Persist/reconcile
            // against your order before fulfillment; statusText is a transient developer summary.
            case .success(let response):
                self?.trace("Outcome: success")
                self?.transactionId = response.transactionId ?? ""
                self?.statusText = "Approved: \(response.transactionId ?? "")"
            case .cancelled:
                self?.trace("Outcome: cancelled")
                self?.statusText = "Customer cancelled"
            case .declined(let response):
                self?.trace("Outcome: declined")
                self?.statusText = "Declined: \(response.processorMessage ?? "")"
            case .failure(let error, let response):
                self?.trace("Outcome: failure")
                self?.statusText = "Payment error: \(error); status: \(response?.status ?? "")"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Card-on-file uses SaveRequest rather than startPayment and ignores the UI amount/tip.
    /// CUSTOMER-1001 is placeholder metadata. The sample shows brand/last4 and retains the vendor
    /// transaction ID; it does not implement customer storage or a complete saved-card workflow.
    func saveCard() {
        guard let credentials = configuredCredentials() else { return }
        let request = SaveRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("save"),
            currency: "USD",
            customId: "CUSTOMER-1001",
            notes: "Card on file",
            credentials: credentials
        )
        guard begin("Save card") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Saving card…"
        BluePosGo.shared.save(request: request) { [weak self] result in
            self?.received("Save card")
            switch result {
            case .success(let response):
                self?.trace("Outcome: success")
                self?.transactionId = response.transactionId ?? ""
                self?.statusText = "Saved \(response.cardBrand ?? "card") ending \(response.cardLast4 ?? "----")"
            case .declined(let response):
                self?.trace("Outcome: declined")
                self?.statusText = "Save declined: \(response.processorMessage ?? "")"
            case .cancelled:
                self?.trace("Outcome: cancelled")
                self?.statusText = "Save cancelled"
            case .failure(let error, _):
                self?.trace("Outcome: failure")
                self?.statusText = "Save failed: \(error)"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Fetch the list available from BluePOS Go. This request has no credential fields in this
    /// SDK; initialize the session first. Replacing this in-memory list is for inspection/ID
    /// selection, not durable order history or a computed refundable-balance ledger.
    func loadTransactions() {
        let request = TransactionListRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("transactions")
        )
        guard begin("Transaction list") else { return }
        trace("Request ID: \(request.requestId ?? "none")")
        statusText = "Loading transactions…"
        BluePosGo.shared.getTransactionList(request: request) { [weak self] result in
            self?.received("Transaction list")
            switch result {
            case .success(let response):
                self?.trace("Outcome: success")
                self?.transactions = response.transactions
                self?.statusText = response.transactions.isEmpty ? "No transactions returned." : "Loaded \(response.transactions.count) transactions. Select one to use its ID."
            case .failure(let error, _):
                self?.trace("Outcome: failure")
                self?.statusText = "Could not load transactions: \(error)"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Refund the referenced vendor transaction using credentials; full refund has no amount
    /// parameter. Do not derive a full-refund amount from the current UI field or transaction row.
    func fullRefund() {
        guard let transactionId = requireTransactionId() else { return }
        guard let credentials = configuredCredentials() else { return }
        let request = FullRefundRequest(
            requestId: UUID().uuidString,
            transactionId: transactionId,
            callbackURL: CallbackPath.url("refund"),
            credentials: credentials
        )
        guard begin("Full refund") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Refunding…"
        BluePosGo.shared.fullRefund(request: request) { [weak self] result in
            self?.received("Full refund")
            switch result {
            case .success(let response):
                self?.trace("Outcome: success")
                self?.statusText = "Refunded: \(response.transactionId ?? transactionId)"
            case .failure(let error, _):
                self?.trace("Outcome: failure")
                self?.statusText = "Refund failed: \(error)"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Partial refund includes the requested positive Decimal amount plus the vendor ID and
    /// credentials. This sample checks format/presence only, not prior refunds or remaining value.
    func partialRefund() {
        guard let transactionId = requireTransactionId() else { return }
        guard let credentials = configuredCredentials() else { return }
        guard let amount = PaymentInput.amount(amount) else {
            statusText = "Enter a valid partial amount."
            return
        }
        let request = PartialRefundRequest(
            requestId: UUID().uuidString,
            transactionId: transactionId,
            amount: amount,
            callbackURL: CallbackPath.url("refund"),
            credentials: credentials
        )
        guard begin("Partial refund") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Partial refund…"
        BluePosGo.shared.partialRefund(request: request) { [weak self] result in
            self?.received("Partial refund")
            switch result {
            case .success(let response):
                self?.trace("Outcome: success")
                self?.statusText = "Partial refund \(response.transactionId ?? ""): \(response.errorMessage ?? "")"
            case .failure(let error, _):
                self?.trace("Outcome: failure")
                self?.statusText = "Partial refund failed: \(error)"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Capture references an existing authorization. Unlike payment/refund, this SDK request
    /// expects a dot-decimal amount STRING and has no credential argument. Keep the POSIX
    /// formatting boundary below even when the user enters a comma or uses a different locale.
    func capture() {
        guard let transactionId = requireTransactionId() else { return }
        guard let amount = PaymentInput.amount(amount) else {
            statusText = "Enter a valid amount greater than zero."
            return
        }
        let formatted = PaymentInput.formatted(amount)
        let request = CaptureRequest(
            requestId: UUID().uuidString,
            transactionId: transactionId,
            amount: formatted,
            callbackURL: CallbackPath.url("capture")
        )
        guard begin("Capture") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Capturing…"
        BluePosGo.shared.capture(request: request) { [weak self] result in
            self?.received("Capture")
            switch result {
            case .success(let response):
                self?.trace("Outcome: success")
                self?.statusText = "Captured: \(response.transactionId ?? "")"
            case .failure(let error, let response):
                self?.trace("Outcome: failure")
                self?.statusText = "Capture failed: \(error); \(response?.errorMessage ?? "")"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Reader demonstration only: nil timeout leaves the SDK default in effect. Clear-data
    /// success may include sensitive card material, so the switch deliberately does not bind
    /// or inspect that payload. Keep it out of status, diagnostics, analytics and persisted state.
    func clearDataRead() {
        guard let amount = PaymentInput.amount(amount) else {
            statusText = "Enter a valid amount greater than zero."
            return
        }
        let request = ClearDataRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("clear-data"),
            amount: amount,
            timeout: nil
        )
        guard begin("Clear-data read") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Clear-data read…"
        BluePosGo.shared.clearData(request: request) { [weak self] result in
            self?.received("Clear-data read")
            switch result {
            case .success:
                self?.trace("Outcome: success")
                self?.statusText = "Clear data received. Not displayed or logged."
            case .masked:
                self?.trace("Outcome: masked")
                self?.statusText = "Completed, but clear data was not eligible."
            case .cancelled:
                self?.trace("Outcome: cancelled")
                self?.statusText = "Clear-data read cancelled."
            case .failure(let error, _):
                self?.trace("Outcome: failure")
                self?.statusText = "Clear-data read failed: \(error)"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }
    }

    /// Ask BluePOS Go to reboot its reader and report the result. This does not pair/connect a
    /// reader; this iOS SDK exposes no connect/disconnect/forget methods (see CheckoutView).
    func rebootReader() {
        let request = RebootRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("reboot")
        )
        guard begin("Reboot") else { return }
        trace("Request ID: \(request.requestId)")
        statusText = "Rebooting reader…"
        BluePosGo.shared.reboot(request: request) { [weak self] result in
            self?.received("Reboot")
            switch result {
            case .success(let response):
                self?.trace("Outcome: success")
                self?.statusText = "Reader rebooted: \(response.readerSerial ?? "unknown")"
            case .failure(let error, _):
                self?.trace("Outcome: failure")
                self?.statusText = "Reboot failed: \(error)"
            @unknown default:
                self?.statusText = "The SDK returned an unrecognized result."
            }
        }

    }

    /// Abandon LOCAL completion tracking, not a payment reversal or remote reader cancellation.
    /// Check the remote outcome before retrying: the original operation may have completed.
    /// Calling all cancel methods is appropriate only because this sample owns all SDK flows
    /// and permits one at a time. An app with multiple clients must cancel only the flow it owns.
    func cancelPending() {
        BluePosGo.shared.cancelPendingPayment()
        BluePosGo.shared.cancelPendingInitialization()
        BluePosGo.shared.cancelPendingTransactionList()
        BluePosGo.shared.cancelPendingFullRefund()
        BluePosGo.shared.cancelPendingPartialRefund()
        BluePosGo.shared.cancelPendingCapture()
        BluePosGo.shared.cancelPendingClearData()
        BluePosGo.shared.cancelPendingReboot()
        BluePosGo.shared.cancelPendingSave()
        isBusy = false
        trace("Local SDK callbacks cleared; payment outcome must be checked in BluePOS Go")
        statusText = "Pending callbacks cleared. Check the payment outcome in BluePOS Go."
    }

}
