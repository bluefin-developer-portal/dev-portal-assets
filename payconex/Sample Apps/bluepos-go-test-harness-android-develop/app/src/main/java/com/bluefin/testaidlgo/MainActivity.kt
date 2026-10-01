package com.bluefin.testaidlgo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.RemoteException
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.bluefin.testaidlgo.data.REFUND
import com.bluefin.testaidlgo.data.createCaptureRequest
import com.bluefin.testaidlgo.data.createClearDataRequest
import com.bluefin.testaidlgo.data.createFullRefundRequest
import com.bluefin.testaidlgo.data.createInitRequest
import com.bluefin.testaidlgo.data.createPaymentRequest
import com.bluefin.testaidlgo.data.createRefundRequest
import com.bluefin.testaidlgo.ui.MainScreen
import com.bluefin.testaidlgo.ui.events.MainScreenEvents
import com.bluefin.testaidlgo.ui.theme.TestAIDLTheme

private const val UNKNOWN_COMMAND = "Unknown command"

// this activity is created to send a sample payment to BluePOS and get the result
class MainActivity : ComponentActivity() {

    private var service: PaymentServiceAIDL? = null
    private var statusText by mutableStateOf("Ready")
    private var debugMode by mutableStateOf(true)
    private var transList by mutableStateOf(listOf<PaymentResponse?>())
    private var refundsList by mutableStateOf(listOf<PaymentResponse>())

    private val paymentCallback = object : PaymentCallback.Stub() {
        override fun onPaymentResult(response: PaymentResponse) {
            runOnUiThread {
                Log.d("Receive result", response.toString())
                statusText = formatPaymentResponse(response)
            }
        }

        override fun onClearDataResult(response: ClearDataResponse?) {
            runOnUiThread {
                Log.d("Receive result", response.toString())
                statusText = response?.let { formatClearDataResponse(it) } ?: "Unknown result"
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun onTransactionsListResponse(list: List<PaymentResponse?>?) {
            statusText = "List received"
            list?.let { lst ->
                transList = lst
                refundsList = lst.filter { it?.type == REFUND } as List<PaymentResponse>
            }
        }

        override fun onInitResult(response: InitResponse?) {
            runOnUiThread {
                statusText = response?.status ?: ""
            }
        }

        override fun onRebootResult(response: InitResponse?) {
            statusText = response?.status ?: ""
        }

        override fun onError(message: String?) {
            runOnUiThread {
                statusText = "Payment error: $message"
            }
        }

        override fun onDeviceCommandResult(response: DeviceCommandResponse?) {
            runOnUiThread {
                statusText = "Device command ${response?.command} result: ${response?.status}"
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = PaymentServiceAIDL.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

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

    private fun formatClearDataResponse(response: ClearDataResponse): String {
        return buildString {
            append("Status: ")
            append(response.status)
        }
    }

    private fun processAction(
        event: MainScreenEvents,
        updateStatus: (String) -> Unit,
        updateChecked: (Boolean) -> Unit
    ) {
        when (event) {
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

            MainScreenEvents.ClearStatusText -> updateStatus("")

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
        enableEdgeToEdge(statusBarStyle = statusBarStyle())
        setContent {
            TestAIDLTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .imePadding()
                        .navigationBarsPadding()
                ) { innerPadding ->
                    MainScreen(
                        statusText = statusText,
                        transList = transList,
                        refundsList = refundsList,
                        checked = debugMode,
                    ) { event ->
                        processAction(
                            event = event,
                            updateStatus = { newStatus -> statusText = newStatus },
                            updateChecked = { newChecked ->
                                debugMode = newChecked
                                service?.debugMode = debugMode
                            }
                        )
                    }
                }
            }
        }
    }

    private fun statusBarStyle(): SystemBarStyle =
        if (isDarkThemeEnabled()) {
            SystemBarStyle.dark(Color.BLACK)
        } else {
            SystemBarStyle.light(
                scrim = Color.WHITE,
                darkScrim = Color.BLACK
            )
        }

    private fun isDarkThemeEnabled(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    override fun onStart() {
        super.onStart()

        val intent = Intent(PAYMENT_SERVICE_ACTION)
        intent.setPackage(BLUEPOS_GO_PACKAGE)

        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        unbindService(connection)
        service = null
    }

    private fun startThroughService(
        command: String,
        request: Any? = null
    ) {
        if (service == null) {
            statusText = UNKNOWN_COMMAND
            return
        }

        try {
            var serviceResult: Boolean? = false
            service?.debugMode = debugMode
            if (service?.isInitialized != true && command != EXTRA_INIT) {
                statusText = getString(R.string.not_initialized)
                return
            }
            when (command) {
                EXTRA_PAYMENT_REQUEST -> request?.let {
                    serviceResult = service?.payment(request as PaymentRequest, paymentCallback)
                } ?: { statusText = UNKNOWN_COMMAND }
                EXTRA_CLEAR_DATA_REQUEST -> request?.let {
                    serviceResult = service?.clearDataRead(request as ClearDataRequest, paymentCallback)
                } ?: { statusText = UNKNOWN_COMMAND }
                EXTRA_FULL_REFUND_REQUEST -> request?.let {
                    serviceResult = service?.fullRefund(request as FullRefundRequest, paymentCallback)
                } ?: { statusText = UNKNOWN_COMMAND }
                EXTRA_REFUND_REQUEST -> request?.let {
                    serviceResult = service?.refund(request as PostProcessRequest, paymentCallback)
                } ?: { statusText = UNKNOWN_COMMAND }
                EXTRA_CAPTURE_REQUEST -> request?.let {
                    serviceResult = service?.capture(request as PostProcessRequest, paymentCallback)
                } ?: { statusText = UNKNOWN_COMMAND }
                EXTRA_INIT -> request?.let {
                    serviceResult = service?.init(request as InitRequest, paymentCallback)
                } ?: { statusText = UNKNOWN_COMMAND }
                EXTRA_TR_LIST_REQUEST ->
                    serviceResult = service?.getTransactionsListResponse(paymentCallback)

                EXTRA_REBOOT -> serviceResult = service?.reboot(paymentCallback)

                EXTRA_CONNECT -> serviceResult = service?.connectDevice(paymentCallback)

                EXTRA_DISCONNECT -> serviceResult = service?.disconnectDevice(paymentCallback)

                EXTRA_FORGET -> serviceResult = service?.forgetDevice(paymentCallback)

                else -> statusText = UNKNOWN_COMMAND
            }
            if (serviceResult == true) {
                openBluePosGoPayment(command, request?.let { it as Parcelable })
                statusText = "Payment started. Waiting for callback..."
            } else
                statusText = "Service is busy or not started"
        } catch (e: RemoteException) {
            statusText = "PaymentService error: ${e.message}"
        } catch (_: ActivityNotFoundException) {
            statusText = "BluePOS Go activity was not found"
        }
    }
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