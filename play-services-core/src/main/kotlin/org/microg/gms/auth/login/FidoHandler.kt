/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.login

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.fido.fido2.api.common.*
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.transport.bluetooth.BluetoothTransportHandler
import org.microg.gms.fido.core.transport.nfc.NfcTransportHandler
import org.microg.gms.fido.core.transport.screenlock.ScreenLockTransportHandler
import org.microg.gms.fido.core.transport.usb.UsbTransportHandler
import org.microg.gms.utils.toBase64

fun JSONObject.getStringOrNull(key: String) = if (has(key)) getString(key) else null
fun JSONObject.getIntOrNull(key: String) = if (has(key)) getInt(key) else null
fun JSONObject.getDoubleOrNull(key: String) = if (has(key)) getDouble(key) else null
fun JSONObject.getArrayOrNull(key: String) = if (has(key)) getJSONArray(key) else null

class FidoHandler(private val activity: LoginActivity) : TransportHandlerCallback {
    private lateinit var requestOptions: PublicKeyCredentialRequestOptions
    private val transportHandlers by lazy {
        setOfNotNull(
            BluetoothTransportHandler(activity, this),
            NfcTransportHandler(activity, this),
            if (SDK_INT >= 21) UsbTransportHandler(activity, this) else null,
            if (SDK_INT >= 23) ScreenLockTransportHandler(activity, this) else null
        )
    }

    override fun onStatusChanged(transport: Transport, status: String, extras: Bundle?) {
        Log.d(TAG, "onStatusChanged: $transport, $status")
    }

    private fun sendEvent(type: String, data: JSONObject, extras: JSONObject? = null) {
        val event = JSONObject(extras?.toString() ?: "{}")
        event.put("type", type)
        event.put("data", data)
        activity.runScript("window.setFido2SkUiEvent($event)")
    }

    private fun sendResult(result: JSONObject) {
        activity.runScript("window.setFido2SkResult($result)")
    }

    private fun sendSelectView(viewName: String, extras: JSONObject? = null) {
        val data = JSONObject(extras?.toString() ?: "{}")
        data.put("viewName", viewName)
        sendEvent("select_view", data)
    }

    private fun sendErrorResult(errorCode: ErrorCode, errorMessage: String?) {
        Log.d(TAG, "Finish with error: $errorMessage ($errorCode)")
        sendResult(JSONObject().apply {
            put("errorCode", errorCode.code)
            if (errorMessage != null) put("errorMessage", errorMessage)
        })
    }

    private fun sendSuccessResult(response: AuthenticatorResponse, transport: Transport) {
        Log.d(TAG, "Finish with success response: $response")
        if (response is AuthenticatorAssertionResponse) {
            sendResult(JSONObject().apply {
                val base64Flags = Base64.NO_PADDING + Base64.NO_WRAP + Base64.URL_SAFE
                put("keyHandle", response.keyHandle?.toBase64(base64Flags))
                put("clientDataJSON", response.clientDataJSON?.toBase64(base64Flags))
                put("authenticatorData", response.authenticatorData?.toBase64(base64Flags))
                put("signature", response.signature?.toBase64(base64Flags))
                if (response.userHandle != null) {
                    put("userHandle", response.userHandle?.toBase64(base64Flags))
                }
            })
        }
    }

    private val availableTransports: List<String>
        get() {
            val list = mutableListOf<String>()
            val transports = transportHandlers.filter { it.isSupported }.map { it.transport }
            if (Transport.BLUETOOTH in transports) {
                list.add("bt")
                list.add("ble")
            }
            if (Transport.USB in transports) list.add("usb")
            if (Transport.NFC in transports) list.add("nfc")
            if (Transport.SCREEN_LOCK in transports) list.add("internal")
            return list
        }

    fun startSignRequest(request: String) {
        try {
            val requestObject = JSONObject(request)
            requestOptions = PublicKeyCredentialRequestOptions.Builder().apply {
                val base64Flags = Base64.NO_PADDING + Base64.NO_WRAP + Base64.URL_SAFE
                requestObject.getStringOrNull("challenge")?.let { setChallenge(Base64.decode(it, base64Flags)) }
                requestObject.getDoubleOrNull("timeoutSeconds")?.let { setTimeoutSeconds(it) }
                requestObject.getStringOrNull("rpId")?.let { setRpId(it) }
                requestObject.getArrayOrNull("allowList")?.let {
                    val allowList = mutableListOf<PublicKeyCredentialDescriptor>()
                    for (i in 0 until it.length()) {
                        val obj = it.getJSONObject(i)
                        allowList.add(
                            PublicKeyCredentialDescriptor(
                                obj.getStringOrNull("type") ?: "public-key",
                                Base64.decode(obj.getString("id"), base64Flags),
                                emptyList()
                            )
                        )
                    }
                    setAllowList(allowList)
                }
                requestObject.getIntOrNull("requestId")?.let { setRequestId(it) }
            }.build()
            Log.d(TAG, "sign: $requestOptions")
            sendSelectView("multiple_transports", JSONObject().apply {
                put("transports", JSONArray(availableTransports))
            })
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    fun onEvent(event: String) {
        try {
            val eventObject = JSONObject(event)
            Log.d(TAG, "event: $eventObject")
            when (eventObject.getString("type")) {
                "user_selected_view_for_transport" -> {
                    val transport = when (eventObject.getJSONObject("data").getString("transport")) {
                        "bt" -> Transport.BLUETOOTH
                        "ble" -> Transport.BLUETOOTH
                        "nfc" -> Transport.NFC
                        "usb" -> Transport.USB
                        "internal" -> Transport.SCREEN_LOCK
                        else -> return
                    }
                    val transportHandler = transportHandlers.firstOrNull { it.transport == transport && it.isSupported } ?: return
                    activity.lifecycleScope.launchWhenStarted {
                        val options = requestOptions
                        try {
                            sendSuccessResult(transportHandler.start(options, activity.packageName), transport)
                        } catch (e: CancellationException) {
                            Log.w(TAG, e)
                            // Ignoring cancellation here
                        } catch (e: RequestHandlingException) {
                            Log.w(TAG, e)
                            sendErrorResult(e.errorCode, e.message)
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                            sendErrorResult(ErrorCode.UNKNOWN_ERR, e.message)
                        }
                    }
                    val extras = JSONObject().apply {
                        put("alternateAvailableTransports", JSONArray(availableTransports))
                    }
                    val viewName = when (transport) {
                        Transport.NFC -> {
                            extras.put("deviceRemovedTooSoon", false)
                            extras.put("recommendUsb", false)
                            "nfc_instructions"
                        }
                        Transport.USB -> "usb_instructions"
                        Transport.BLUETOOTH -> "ble_instructions"
                        else -> return
                    }
                    sendSelectView(viewName, extras)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    fun cancel() {
        Log.d(TAG, "cancel")
    }

    companion object {
        private const val TAG = "AuthFidoHandler"
    }
}
