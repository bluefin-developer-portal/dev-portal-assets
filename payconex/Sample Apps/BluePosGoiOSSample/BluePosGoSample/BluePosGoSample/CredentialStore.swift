import BluePosGoSDK
import Foundation

struct CredentialStore {
    var basicToken: String
    var accountId: String
    var environment: String

    var credentials: PaymentCredentials {
        PaymentCredentials(
            basicToken: basicToken,
            accountId: accountId,
            environment: environment
        )
    }
}

enum CallbackPath {
    static let scheme = "mymerchantapp"

    static func url(_ path: String) -> URL {
        URL(string: "\(scheme)://bluepos/\(path)")!
    }
}
