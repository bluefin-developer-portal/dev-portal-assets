import BluePosGoSDK
import Foundation

final class CheckoutModel: ObservableObject {
    @Published var basicToken = ""
    @Published var accountId = ""
    @Published var environment = "STAGING"
    @Published var amount = "19.99"
    @Published var tip = "2.00"
    @Published var transactionId = ""
    @Published var statusText = "Ready. Initialize before taking a payment."
    @Published var demoOutcome = "approved"

    private var credentials: PaymentCredentials {
        PaymentCredentials(
            basicToken: basicToken.trimmingCharacters(in: .whitespacesAndNewlines),
            accountId: accountId.trimmingCharacters(in: .whitespacesAndNewlines),
            environment: environment
        )
    }

    func initialize() {
        let request = InitializationRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("initialize"),
            basicToken: credentials.basicToken,
            accountId: credentials.accountId,
            environment: credentials.environment
        )
        statusText = "Initializing…"
        BluePosGo.shared.initialize(request: request) { [weak self] result in
            switch result {
            case .initialized(let response):
                self?.statusText = "Initialized: \(response.status ?? "unknown")"
            case .failure(let error, let response):
                self?.statusText = "Initialization failed: \(error); \(response?.message ?? "")"
            }
        }
        
    }

    func takePayment(type: PaymentTransactionType) {
        guard let amount = Decimal(string: amount) else {
            statusText = "Enter a valid amount."
            return
        }
        let request = PaymentRequest(
            requestId: UUID().uuidString,
            amount: amount,
            tip: Decimal(string: tip),
            transactionType: type,
            currency: "USD",
            callbackURL: CallbackPath.url("payment"),
            customId: "ORDER-1001",
            notes: "In-store sale",
            credentials: credentials,
            requiresReaderReady: true
        )
        statusText = "Payment started. Waiting for BluePOS Go…"
        BluePosGo.shared.startPayment(request: request) { [weak self] result in
            switch result {
            case .success(let response):
                self?.transactionId = response.transactionId ?? ""
                self?.statusText = "Approved: \(response.transactionId ?? "")"
            case .cancelled:
                self?.statusText = "Customer cancelled"
            case .declined(let response):
                self?.statusText = "Declined: \(response.processorMessage ?? "")"
            case .failure(let error, let response):
                self?.statusText = "Payment error: \(error); status: \(response?.status ?? "")"
            }
        }
        
    }

    func saveCard() {
        let request = SaveRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("save"),
            currency: "USD",
            customId: "CUSTOMER-1001",
            notes: "Card on file",
            credentials: credentials
        )
        statusText = "Saving card…"
        BluePosGo.shared.save(request: request) { [weak self] result in
            switch result {
            case .success(let response):
                self?.transactionId = response.transactionId ?? ""
                self?.statusText = "Saved \(response.cardBrand ?? "card") ending \(response.cardLast4 ?? "----")"
            case .declined(let response):
                self?.statusText = "Save declined: \(response.processorMessage ?? "")"
            case .cancelled:
                self?.statusText = "Save cancelled"
            case .failure(let error, _):
                self?.statusText = "Save failed: \(error)"
            }
        }
        
    }

    func loadTransactions() {
        let request = TransactionListRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("transactions")
        )
        statusText = "Loading transactions…"
        BluePosGo.shared.getTransactionList(request: request) { [weak self] result in
            switch result {
            case .success(let response):
                let ids = response.transactions.compactMap { $0.transactionDetails.transactionId }
                self?.statusText = ids.isEmpty ? "No transactions yet." : ids.joined(separator: "\n")
            case .failure(let error, _):
                self?.statusText = "Could not load transactions: \(error)"
            }
        }
        
    }

    func fullRefund() {
        let request = FullRefundRequest(
            requestId: UUID().uuidString,
            transactionId: transactionId,
            callbackURL: CallbackPath.url("refund"),
            credentials: credentials
        )
        statusText = "Refunding…"
        BluePosGo.shared.fullRefund(request: request) { [weak self] result in
            if case .success(let response) = result {
                self?.statusText = "Refunded: \(response.transactionId ?? self?.transactionId ?? "")"
            } else if case .failure(let error, _) = result {
                self?.statusText = "Refund failed: \(error)"
            }
        }
        
    }

    func partialRefund() {
        guard let amount = Decimal(string: amount) else {
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
        statusText = "Partial refund…"
        BluePosGo.shared.partialRefund(request: request) { [weak self] result in
            switch result {
            case .success(let response):
                self?.statusText = "Partial refund \(response.transactionId ?? ""): \(response.processorMessage ?? "")"
            case .failure(let error, _):
                self?.statusText = "Partial refund failed: \(error)"
            }
        }
        
    }

    func capture() {
        let formatted = String(format: "%.2f", locale: Locale(identifier: "en_US_POSIX"), (Decimal(string: amount) as NSDecimalNumber?)?.doubleValue ?? 0)
        let request = CaptureRequest(
            requestId: UUID().uuidString,
            transactionId: transactionId,
            amount: formatted,
            callbackURL: CallbackPath.url("capture")
        )
        statusText = "Capturing…"
        BluePosGo.shared.capture(request: request) { [weak self] result in
            switch result {
            case .success(let response):
                self?.statusText = "Captured: \(response.transactionId ?? "")"
            case .failure(let error, let response):
                self?.statusText = "Capture failed: \(error); \(response?.processorMessage ?? "")"
            }
        }
        
    }

    func clearDataRead() {
        let request = ClearDataRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("clear-data"),
            amount: Decimal(string: amount),
            timeout: nil
        )
        statusText = "Clear-data read…"
        BluePosGo.shared.clearData(request: request) { [weak self] result in
            switch result {
            case .success:
                self?.statusText = "Clear data received. Not displayed or logged."
            case .masked:
                self?.statusText = "Completed, but clear data was not eligible."
            case .cancelled:
                self?.statusText = "Clear-data read cancelled."
            case .failure(let error):
                self?.statusText = "Clear-data read failed: \(error)"
            }
        }
        /// simulateReturn(to: request.callbackURL, requestId: request.requestId, forceStatus: demoOutcome == "masked" ? "masked" : nil)
    }

    func rebootReader() {
        let request = RebootRequest(
            requestId: UUID().uuidString,
            callbackURL: CallbackPath.url("reboot")
        )
        statusText = "Rebooting reader…"
        BluePosGo.shared.reboot(request: request) { [weak self] result in
            switch result {
            case .success(let response):
                self?.statusText = "Reader rebooted: \(response.readerSerial ?? "unknown")"
            case .failure(let error, _):
                self?.statusText = "Reboot failed: \(error)"
            }
        }
        
    }

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
        statusText = "Pending SDK operations cleared."
    }

}
