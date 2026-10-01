package com.bluefin.testaidlgo.ui.events

sealed interface MainScreenEvents {
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