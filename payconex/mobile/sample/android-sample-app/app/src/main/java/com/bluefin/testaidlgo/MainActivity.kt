package com.bluefin.testaidlgo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import com.bluefin.blueposgo.sdk.ACTION_EXTERNAL
import com.bluefin.blueposgo.sdk.BLUEPOS_GO_ACTIVITY
import com.bluefin.blueposgo.sdk.BLUEPOS_GO_PACKAGE
import com.bluefin.blueposgo.sdk.EXTRA_CAPTURE_REQUEST
import com.bluefin.blueposgo.sdk.EXTRA_CLEAR_DATA_REQUEST
import com.bluefin.blueposgo.sdk.EXTRA_COMMAND
import com.bluefin.blueposgo.sdk.EXTRA_CONNECT
import com.bluefin.blueposgo.sdk.EXTRA_DISCONNECT
import com.bluefin.blueposgo.sdk.EXTRA_FORGET
import com.bluefin.blueposgo.sdk.EXTRA_FULL_REFUND_REQUEST
import com.bluefin.blueposgo.sdk.EXTRA_INIT
import com.bluefin.blueposgo.sdk.EXTRA_PAYLOAD
import com.bluefin.blueposgo.sdk.EXTRA_PAYMENT_REQUEST
import com.bluefin.blueposgo.sdk.EXTRA_REBOOT
import com.bluefin.blueposgo.sdk.EXTRA_REFUND_REQUEST
import com.bluefin.blueposgo.sdk.EXTRA_TR_LIST_REQUEST
import com.bluefin.blueposgo.sdk.PAYMENT_SERVICE_ACTION
import com.bluefin.blueposgo.sdk.PaymentCallback
import com.bluefin.blueposgo.sdk.PaymentServiceAIDL
import com.bluefin.blueposgo.sdk.request.ClearDataRequest
import com.bluefin.blueposgo.sdk.request.FullRefundRequest
import com.bluefin.blueposgo.sdk.request.InitRequest
import com.bluefin.blueposgo.sdk.request.PaymentRequest
import com.bluefin.blueposgo.sdk.request.PostProcessRequest
import com.bluefin.blueposgo.sdk.response.ClearDataResponse
import com.bluefin.blueposgo.sdk.response.DeviceCommandResponse
import com.bluefin.blueposgo.sdk.response.InitResponse
import com.bluefin.blueposgo.sdk.response.PaymentResponse
import com.bluefin.testaidlgo.data.createCaptureRequest
import com.bluefin.testaidlgo.data.createClearDataRequest
import com.bluefin.testaidlgo.data.createFullRefundRequest
import com.bluefin.testaidlgo.data.createInitRequest
import com.bluefin.testaidlgo.data.createPaymentRequest
import com.bluefin.testaidlgo.data.createRefundRequest
import com.bluefin.testaidlgo.ui.MainScreen
import com.bluefin.testaidlgo.ui.events.MainScreenEvents
import com.bluefin.testaidlgo.ui.theme.BluefinSampleTheme

private const val UNKNOWN_COMMAND = "Unknown command"

/**
 * Android integration entry point: MainScreen emits an event, processAction builds the SDK
 * request, startThroughService submits it over AIDL, and BluePOS Go owns the reader UI.
 * Results arrive on paymentCallback; returning to this activity alone is not a payment result.
 * Read data/PaymentHelpers.kt alongside this file for credentials and amount conversion.
 *
 * This deliberately small sample keeps connection and result state in the Activity. A merchant
 * app needs a durable order/operation record and recovery after recreation or process death;
 * the displayed status and transaction list are not a transaction ledger. For a testable app,
 * extract the SDK calls behind an injected gateway and keep checkout state outside the view.
 */
class MainActivity : ComponentActivity() {

    // A service proxy means IPC is connected, not that initialization or reader pairing succeeded.
    // The callback, list, status and diagnostics live only as long as this Activity instance.
    private var service: PaymentServiceAIDL? = null
    private var statusText by mutableStateOf("Ready. Initialize before taking a payment.")
    private var debugMode by mutableStateOf(false)
    private var darkMode by mutableStateOf(true)
    private var transList by mutableStateOf(listOf<PaymentResponse>())
    private var transactionId by mutableStateOf("")
    private var diagnosticsEnabled by mutableStateOf(true)
    private var diagnostics by mutableStateOf(listOf<String>())
    private var connectionStatus by mutableStateOf("BluePOS Go service: disconnected")
    // bindService can succeed before onServiceConnected runs. Track the binding separately
    // from the nullable proxy so onStop also unbinds a connection that is still being established.
    private var serviceBound = false
    // This checks local presence/environment spelling only. BluePOS Go validates credentials
    // when initialized; a configured UI must not be treated as an authenticated session.
    private val isConfigured: Boolean
        get() = listOf(BuildConfig.BLUEPOS_ACCOUNT_ID, BuildConfig.BLUEPOS_API_KEY,
            BuildConfig.BLUEPOS_API_SECRET).all { it.isNotBlank() } &&
            BuildConfig.BLUEPOS_ENVIRONMENT in listOf("STAGING", "CERT", "PROD")

    /**
     * Bounded, in-memory integration trace, separate from the SDK's debugMode flag.
     * Call on the UI thread and pass only deliberately selected metadata. Never interpolate a
     * request, full response, Basic token or clear-card payload; those may contain sensitive data.
     */
    private fun trace(event: String) {
        if (diagnosticsEnabled) {
            val time = java.time.LocalTime.now().withNano(0)
            diagnostics = (diagnostics + "$time  $event").takeLast(50)
        }
    }

    // Binder callbacks can arrive on Binder threads. Marshal every Compose-state update to
    // the UI thread. Keep callbacks lightweight; any durable reconciliation belongs elsewhere.
    // PaymentResponse covers multiple payment/post-processing actions, so keep the operation
    // context in your own app when you need to distinguish sale, authorization, capture or refund.
    private val paymentCallback = object : PaymentCallback.Stub() {
        override fun onPaymentResult(response: PaymentResponse) {
            runOnUiThread {
                trace("Payment callback; error code ${response.errorCode}")
                response.transactionDetails?.transactionId?.takeIf { it.isNotBlank() }?.let { transactionId = it }
                statusText = formatPaymentResponse(response)
            }
        }

        override fun onClearDataResult(response: ClearDataResponse?) {
            runOnUiThread {
                trace("Clear-data callback received; payload omitted")
                statusText = response?.let { formatClearDataResponse(it) } ?: "Unknown result"
            }
        }

        override fun onTransactionsListResponse(list: List<PaymentResponse?>?) {
            runOnUiThread {
                transList = list.orEmpty().filterNotNull()
                trace("Transaction list callback; ${transList.size} entries")
                statusText = if (transList.isEmpty()) "No transactions returned." else "Loaded ${transList.size} transactions. Select one to use its ID."
            }
        }

        override fun onInitResult(response: InitResponse?) {
            runOnUiThread {
                trace("Initialization callback received")
                statusText = response?.status ?: "No initialization result"
            }
        }

        override fun onRebootResult(response: InitResponse?) {
            runOnUiThread {
                trace("Reboot callback received")
                statusText = response?.status ?: "No reboot result"
            }
        }

        override fun onError(message: String?) {
            runOnUiThread {
                trace("SDK error callback received")
                statusText = "SDK error: $message"
            }
        }

        override fun onDeviceCommandResult(response: DeviceCommandResponse?) {
            runOnUiThread {
                trace("Reader command callback received")
                statusText = "Device command ${response?.command} result: ${response?.status}"
            }
        }
    }

    // Binding and SDK initialization are separate steps. A disconnect clears only the proxy;
    // it does not establish whether an in-flight payment succeeded or failed.
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = PaymentServiceAIDL.Stub.asInterface(binder)
            connectionStatus = "BluePOS Go service: connected"
            trace("AIDL service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            connectionStatus = "BluePOS Go service: disconnected"
            trace("AIDL service disconnected")
        }
    }

    /**
     * Human-readable SDK summary only. errorCode == 0 is not itself an approval: inspect the
     * returned transaction status. Production order state must not be derived from this string.
     */
    private fun formatPaymentResponse(response: PaymentResponse): String {
        if (response.errorCode != 0)
            return "Status: Failed\nDescription: ${response.processorMessage}"

        val details = response.transactionDetails
            ?: return "TransactionDetails was not returned"

        return buildString {
            append("Status: ")
            append(details.status)
            append("\nDescription: ")
            append(details.description.orEmpty())
        }
    }

    // Clear-data responses can carry card data. Render the status only; do not add toString()
    // logging or expose the payload through the general-purpose diagnostics panel.
    private fun formatClearDataResponse(response: ClearDataResponse): String {
        return buildString {
            append("Status: ")
            append(response.status)
        }
    }

    /**
     * Maps each UI intent to its vendor request and command. All event amounts/tips are cents;
     * only PaymentHelpers converts to the major-unit Double fields required by this SDK.
     * New operations need an event, a request factory if applicable, and a matching dispatch arm.
     */
    private fun processAction(
        event: MainScreenEvents,
        updateStatus: (String) -> Unit,
        updateChecked: (Boolean) -> Unit
    ) {
        when (event) {
            is MainScreenEvents.DarkModeChange -> {
                darkMode = event.enabled
                // KTX edit applies asynchronously by default, preserving the same write behavior.
                getSharedPreferences("sample_preferences", MODE_PRIVATE).edit {
                    putBoolean("dark_mode", darkMode)
                }
                applySystemBarAppearance()
            }
            is MainScreenEvents.DiagnosticsChange -> diagnosticsEnabled = event.enabled
            MainScreenEvents.ClearDiagnostics -> diagnostics = emptyList()
            is MainScreenEvents.ProcessClick -> {
                val request = createPaymentRequest(event.amount, event.tip)
                startThroughService(EXTRA_PAYMENT_REQUEST, request)
            }

            is MainScreenEvents.AuthClick -> {
                val request = createPaymentRequest(event.amount, event.tip)
                request.type = "auth"
                startThroughService(EXTRA_PAYMENT_REQUEST, request)
            }

            is MainScreenEvents.SaveCardClick -> {
                // Android uses the payment endpoint for save, with a zero amount/tip and type
                // "save". This does not use the amount field or run a normal zero-dollar sale.
                val request = createPaymentRequest(0, 0)
                request.type = "save"
                startThroughService(EXTRA_PAYMENT_REQUEST, request)
            }

            is MainScreenEvents.ClearDataClick -> {
                val request = createClearDataRequest(event.amount)
                startThroughService(EXTRA_CLEAR_DATA_REQUEST, request)
            }

            is MainScreenEvents.InitClick -> {
                val request = createInitRequest()
                startThroughService(EXTRA_INIT, request)
            }

            MainScreenEvents.GetTrListClick ->
                startThroughService(EXTRA_TR_LIST_REQUEST, null)

            is MainScreenEvents.FullRefundClick -> {
                val request = createFullRefundRequest(event.id)
                startThroughService(EXTRA_FULL_REFUND_REQUEST, request)
            }

            MainScreenEvents.ClearStatusText -> updateStatus("No result yet.")

            is MainScreenEvents.DebugChange -> updateChecked(event.checked)

            is MainScreenEvents.RefundClick ->  {
                val request = createRefundRequest(event.transactionId, event.amount)
                startThroughService(EXTRA_REFUND_REQUEST, request)
            }

            is MainScreenEvents.CaptureClick ->  {
                val request = createCaptureRequest(event.transactionId, event.amount)
                startThroughService(EXTRA_CAPTURE_REQUEST, request)
            }

            MainScreenEvents.RebootClick -> startThroughService(EXTRA_REBOOT, null)

            MainScreenEvents.ConnectClick -> startThroughService(EXTRA_CONNECT, null)

            MainScreenEvents.DisconnectClick -> startThroughService(EXTRA_DISCONNECT, null)

            MainScreenEvents.ForgetClick -> startThroughService(EXTRA_FORGET, null)
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Suppress("unused")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only appearance is persisted in preferences. Missing preference means dark; an
        // explicit false means the user chose light. Keep credentials out of this UI store.
        darkMode = getSharedPreferences("sample_preferences", MODE_PRIVATE).getBoolean("dark_mode", true)
        transactionId = savedInstanceState?.getString("transactionId").orEmpty()
        if (!isConfigured) statusText = "Payment configuration is missing or invalid. Update payment.properties and rebuild."
        applySystemBarAppearance()
        setContent {
            BluefinSampleTheme(darkTheme = darkMode) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .imePadding()
                        .navigationBarsPadding()
                ) { innerPadding ->
                    MainScreen(
                        statusText = statusText,
                        transList = transList,
                        transactionId = transactionId,
                        onTransactionIdChange = { transactionId = it },
                        isConfigured = isConfigured,
                        checked = debugMode,
                        darkMode = darkMode,
                        diagnosticsEnabled = diagnosticsEnabled,
                        diagnostics = diagnostics,
                        connectionStatus = connectionStatus,
                    ) { event ->
                        try { processAction(
                            event = event,
                            updateStatus = { newStatus -> statusText = newStatus },
                            updateChecked = { newChecked ->
                                debugMode = newChecked
                                service?.debugMode = debugMode
                                trace("SDK debug mode ${if (debugMode) "enabled" else "disabled"}")
                            }
                        ) } catch (_: Exception) {
                            statusText = "Could not send the SDK request. Check configuration and the BluePOS Go connection."
                            trace("Request rejected before handoff")
                        }
                    }
                }
            }
        }
    }

    // Compose colors and system bars must use the same app preference, even when the device
    // appearance differs. Theme.kt and MainScreen's logo selection use this same darkMode value.
    private fun applySystemBarAppearance() {
        enableEdgeToEdge(statusBarStyle = statusBarStyle(), navigationBarStyle = statusBarStyle())
    }

    private fun statusBarStyle(): SystemBarStyle =
        if (darkMode) {
            SystemBarStyle.dark(Color.BLACK)
        } else {
            SystemBarStyle.light(
                scrim = Color.WHITE,
                darkScrim = Color.BLACK
            )
        }

    // Rebind when the sample becomes visible, including the return from BluePOS Go. Use the
    // SDK's package/action constants and the visibility declarations in AndroidManifest.xml;
    // these identify the vendor app and should not change when renaming your merchant app.
    override fun onStart() {
        super.onStart()

        val intent = Intent(PAYMENT_SERVICE_ACTION)
        intent.setPackage(BLUEPOS_GO_PACKAGE)

        serviceBound = bindService(intent, connection, BIND_AUTO_CREATE)
        if (!serviceBound) {
            connectionStatus = "BluePOS Go service: unavailable (install BluePOS Go)"
            trace("AIDL service unavailable")
        }
    }

    // Match the Activity-owned bind to its visible lifetime. Unbinding is local cleanup, not
    // cancellation of a remote payment. Do not assume this in-memory callback survives Activity
    // recreation/process death; add operation recovery before adapting this for a real checkout.
    override fun onStop() {
        super.onStop()

        if (serviceBound) unbindService(connection)
        serviceBound = false
        service = null
        connectionStatus = "BluePOS Go service: disconnected"
    }

    // Restore the selected ID for UI convenience only. This bundle does not persist pending
    // operations, results or the service callback, and is not a substitute for order storage.
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("transactionId", transactionId)
        super.onSaveInstanceState(outState)
    }

    /**
     * Two-step vendor flow: submit a typed request plus callback through AIDL, then foreground
     * BluePOS Go with the same command/payload only if the service accepts the request.
     * The returned Boolean describes dispatch acceptance; payment approval arrives later.
     *
     * This compact dispatcher uses Any/Parcelable casts. Every command must be paired with the
     * correct request class in processAction. Prefer a typed gateway if extending the sample.
     * These Binder calls run synchronously on the caller (here the UI thread); a larger app
     * should isolate potentially blocking IPC and marshal UI updates back to the main thread.
     */
    private fun startThroughService(
        command: String,
        request: Any? = null
    ) {
        trace("Request: $command")
        if (service == null) {
            trace("Request not sent: service unavailable")
            statusText = "BluePOS Go is unavailable. Install and open BluePOS Go on this device."
            return
        }

        try {
            var serviceResult: Boolean? = false
            // This is the real Android SDK flag, independent of the sample's local trace.
            // Reapply on dispatch because the service connection can be replaced.
            service?.debugMode = debugMode
            // Check remote initialization on each dispatch; local credential presence and a
            // successful bind do not mean that the BluePOS Go payment session is initialized.
            if (service?.isInitialized != true && command != EXTRA_INIT) {
                statusText = getString(R.string.not_initialized)
                return
            }
            when (command) {
                EXTRA_PAYMENT_REQUEST -> request?.let {
                    serviceResult = service?.payment(request as PaymentRequest, paymentCallback)
                } ?: run { statusText = UNKNOWN_COMMAND }
                EXTRA_CLEAR_DATA_REQUEST -> request?.let {
                    serviceResult = service?.clearDataRead(request as ClearDataRequest, paymentCallback)
                } ?: run { statusText = UNKNOWN_COMMAND }
                EXTRA_FULL_REFUND_REQUEST -> request?.let {
                    serviceResult = service?.fullRefund(request as FullRefundRequest, paymentCallback)
                } ?: run { statusText = UNKNOWN_COMMAND }
                EXTRA_REFUND_REQUEST -> request?.let {
                    serviceResult = service?.refund(request as PostProcessRequest, paymentCallback)
                } ?: run { statusText = UNKNOWN_COMMAND }
                EXTRA_CAPTURE_REQUEST -> request?.let {
                    serviceResult = service?.capture(request as PostProcessRequest, paymentCallback)
                } ?: run { statusText = UNKNOWN_COMMAND }
                EXTRA_INIT -> request?.let {
                    serviceResult = service?.init(request as InitRequest, paymentCallback)
                } ?: run { statusText = UNKNOWN_COMMAND }
                EXTRA_TR_LIST_REQUEST ->
                    serviceResult = service?.getTransactionsListResponse(paymentCallback)

                EXTRA_REBOOT -> serviceResult = service?.reboot(paymentCallback)

                EXTRA_CONNECT -> serviceResult = service?.connectDevice(paymentCallback)

                EXTRA_DISCONNECT -> serviceResult = service?.disconnectDevice(paymentCallback)

                EXTRA_FORGET -> serviceResult = service?.forgetDevice(paymentCallback)

                else -> statusText = UNKNOWN_COMMAND
            }
            // Do not mark an order paid here. A later SDK callback can report decline, error,
            // cancellation or success. Missing callbacks/errors also do not justify a blind retry.
            if (serviceResult == true) {
                openBluePosGoPayment(command, request?.let { it as Parcelable })
                trace("Request accepted; opening BluePOS Go")
                statusText = "Request started. Waiting for BluePOS Go…"
            } else
                statusText = "Service is busy or not started"
        } catch (e: RemoteException) {
            // IPC can fail after work was accepted. Reconcile the outcome before resubmitting.
            statusText = "PaymentService error: ${e.message}"
        } catch (_: ActivityNotFoundException) {
            // The service may already have accepted work before this UI launch failed.
            statusText = "BluePOS Go activity was not found"
        }
    }

    /**
     * Opens the SDK-defined BluePOS Go activity; the app switch is part of this integration.
     * Keep EXTRA_COMMAND and EXTRA_PAYLOAD identical to the request accepted over AIDL.
     * Opening the vendor UI does not replace the callback registration above.
     */
    private fun openBluePosGoPayment(
        command: String,
        request: Parcelable?
    ) {
        val intent = Intent(ACTION_EXTERNAL)
            .setClassName(BLUEPOS_GO_PACKAGE, BLUEPOS_GO_ACTIVITY)
            .putExtra(EXTRA_COMMAND, command)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        request?.let {
            intent.putExtra(EXTRA_PAYLOAD, request)
        }

        startActivity(intent)
    }
}