package com.bluefin.testaidlgo.ui.events

/**
 * Explicit UI intents consumed by MainActivity.processAction. All amount/tip Longs are cents,
 * never floating-point dollars; transaction IDs are vendor references, not local order IDs.
 * Credentials belong in the request adapter, not the UI or these events. A fake event consumer
 * can exercise UI behavior without a service, but this sample does not include a mock gateway.
 */
sealed interface MainScreenEvents {
    data class DarkModeChange(val enabled: Boolean): MainScreenEvents
    data class DiagnosticsChange(val enabled: Boolean): MainScreenEvents
    data object ClearDiagnostics: MainScreenEvents
    data class ProcessClick(val amount: Long, val tip: Long): MainScreenEvents
    data class AuthClick(val amount: Long, val tip: Long): MainScreenEvents
    data class ClearDataClick(val amount: Long): MainScreenEvents
    data class FullRefundClick(val id: String): MainScreenEvents
    data class RefundClick(val transactionId: String, val amount: Long): MainScreenEvents
    data class CaptureClick(val transactionId: String, val amount: Long): MainScreenEvents
    data object SaveCardClick: MainScreenEvents
    data object InitClick: MainScreenEvents
    data object RebootClick: MainScreenEvents
    data object GetTrListClick: MainScreenEvents
    data object ClearStatusText: MainScreenEvents

    data object ConnectClick: MainScreenEvents

    data object DisconnectClick: MainScreenEvents

    data object ForgetClick: MainScreenEvents

    data class DebugChange(val checked: Boolean): MainScreenEvents
}