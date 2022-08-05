/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.fido.fido2.api.common.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.protocol.AttestedCredentialData
import org.microg.gms.fido.core.protocol.AuthenticatorData
import org.microg.gms.fido.core.protocol.CoseKey
import org.microg.gms.fido.core.protocol.FidoU2fAttestationObject
import org.microg.gms.fido.core.protocol.msgs.U2fAuthenticationCommand
import org.microg.gms.fido.core.protocol.msgs.U2fRegistrationCommand
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidConnection
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidMessageStatusException

@RequiresApi(21)
class UsbTransportHandler(private val context: Context, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.USB, callback) {
    override val isSupported: Boolean
        get() = context.packageManager.hasSystemFeature("android.hardware.usb.host") && context.usbManager != null

    private val devicePermissionManager by lazy { UsbDevicePermissionManager(context) }

    private var device: UsbDevice? = null

    private infix fun <T> List<T>.eq(other: List<T>): Boolean =
        other.size == size && zip(other).all { (x, y) -> x == y }

    suspend fun getCtapHidInterface(device: UsbDevice): UsbInterface? {
        for (iface in device.interfaces) {
            if (iface.interfaceClass != USB_CLASS_HID) continue
            if (iface.interfaceSubclass != 0) continue
            if (iface.interfaceProtocol != 0) continue
            if (iface.endpointCount != 2) continue
            if (!iface.endpoints.all { it.type == USB_ENDPOINT_XFER_INT }) continue
            if (!iface.endpoints.any { it.direction == USB_DIR_IN }) continue
            if (!iface.endpoints.any { it.direction == USB_DIR_OUT }) continue

            Log.d(TAG, "${device.productName} has suitable hid interface ${iface.id}")
            if (!devicePermissionManager.awaitPermission(device)) continue
            Log.d(TAG, "${device.productName} has permission")
            val match = context.usbManager?.openDevice(device)?.use { connection ->
                if (connection.claimInterface(iface, true)) {
                    val buf = ByteArray(256)
                    val read = connection.controlTransfer(0x81, 0x06, 0x2200, iface.id, buf, buf.size, 5000)
                    read >= 5 && buf.slice(0 until 5) eq CTAPHID_SIGNATURE
                } else {
                    false
                }
            } == true
            if (match) {
                return iface
            }
        }
        return null
    }

    suspend fun deviceHasCredential(
        connection: CtapHidConnection,
        challenge: ByteArray,
        application: ByteArray,
        descriptor: PublicKeyCredentialDescriptor
    ): Boolean {
        try {
            connection.runCommand(U2fAuthenticationCommand(0x07, challenge, application, descriptor.id))
            return true
        } catch (e: CtapHidMessageStatusException) {
            return false
        }
    }

    suspend fun register(
        options: RequestOptions,
        callerPackage: String,
        device: UsbDevice,
        iface: UsbInterface
    ): AuthenticatorAttestationResponse {
        val (response, clientData) = CtapHidConnection(context, device, iface).open {
            if (it.hasCtap2Support) {
                // Not yet supported on our side
            }
            if (it.hasCtap1Support) {
                val rpIdHash = options.rpId.toByteArray().digest("SHA-256")
                val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
                if (!options.registerOptions.parameters.isNullOrEmpty() && options.registerOptions.parameters.all { it.algorithmIdAsInteger != -7 }) throw IllegalArgumentException(
                    "Can't use CTAP1 protocol for non ES256 requests"
                )
                if (options.registerOptions.authenticatorSelection.requireResidentKey == true) throw IllegalArgumentException("Can't use CTAP1 protocol when resident key required")
                val hasCredential = options.registerOptions.excludeList.any { cred ->
                    deviceHasCredential(it, clientDataHash, rpIdHash, cred)
                }
                while (true) {
                    try {
                        val response = it.runCommand(U2fRegistrationCommand(clientDataHash, rpIdHash)) to clientData
                        if (hasCredential) throw RequestHandlingException(
                            ErrorCode.NOT_ALLOWED_ERR,
                            "An excluded credential has already been registered with the device"
                        )
                        return@open response
                    } catch (e: CtapHidMessageStatusException) {
                        if (e.status != 0x6985.toShort()) {
                            throw e
                        }
                    }
                    delay(100)
                }
            }
            throw IllegalStateException()
        }
        require(response.userPublicKey[0] == 0x04.toByte())
        val coseKey = CoseKey(
            EC2Algorithm.ES256,
            response.userPublicKey.sliceArray(1 until 33),
            response.userPublicKey.sliceArray(33 until 65),
            1
        )
        val credentialData =
            AttestedCredentialData(ByteArray(16), response.keyHandle, coseKey.encode())
        val authData = AuthenticatorData(options.rpId.toByteArray().digest("SHA-256"), true, false, 0, credentialData)
        val attestationObject =
            FidoU2fAttestationObject(authData, response.signature, response.attestationCertificate)

        return AuthenticatorAttestationResponse(
            response.keyHandle,
            clientData,
            attestationObject.encode()
        )
    }

    suspend fun sign(
        options: RequestOptions,
        callerPackage: String,
        device: UsbDevice,
        iface: UsbInterface
    ): AuthenticatorAssertionResponse {
        val (response, clientData, credentialId) = CtapHidConnection(context, device, iface).open {
            if (it.hasCtap2Support) {
                // Not yet supported on our side
            }
            if (it.hasCtap1Support) {
                val rpIdHash = options.rpId.toByteArray().digest("SHA-256")
                val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
                val cred = options.signOptions.allowList.firstOrNull { cred ->
                    deviceHasCredential(it, clientDataHash, rpIdHash, cred)
                } ?: options.signOptions.allowList.first()

                while (true) {
                    try {
                        return@open Triple(
                            it.runCommand(
                                U2fAuthenticationCommand(0x03, clientDataHash, rpIdHash, cred.id)
                            ), clientData, cred.id
                        )
                    } catch (e: CtapHidMessageStatusException) {
                        if (e.status != 0x6985.toShort()) {
                            throw e
                        }
                        delay(100)
                    }
                }
            }
            throw IllegalStateException()
        }
        val authData = AuthenticatorData(
            options.rpId.toByteArray().digest("SHA-256"),
            response.userPresence,
            false,
            response.counter
        )

        return AuthenticatorAssertionResponse(
            credentialId,
            clientData,
            authData.encode(),
            response.signature,
            null
        )
    }

    private suspend fun waitForNewUsbDevice(): UsbDevice {
        val deferred = CompletableDeferred<UsbDevice>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
                deferred.complete(device)
            }
        }
        context.registerReceiver(receiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        invokeStatusChanged(TransportHandlerCallback.STATUS_WAITING_FOR_DEVICE)
        return deferred.await()
    }

    suspend fun handle(
        options: RequestOptions,
        callerPackage: String,
        device: UsbDevice,
        iface: UsbInterface
    ): AuthenticatorResponse {
        invokeStatusChanged(
            TransportHandlerCallback.STATUS_WAITING_FOR_USER,
            Bundle().apply { putParcelable(UsbManager.EXTRA_DEVICE, device) })
        try {
            return when (options.type) {
                RequestOptionsType.REGISTER -> register(options, callerPackage, device, iface)
                RequestOptionsType.SIGN -> sign(options, callerPackage, device, iface)
            }
        } finally {
            this.device = null
        }
    }

    override suspend fun start(options: RequestOptions, callerPackage: String): AuthenticatorResponse {
        for (device in context.usbManager?.deviceList?.values.orEmpty()) {
            val iface = getCtapHidInterface(device) ?: continue
            try {
                return handle(options, callerPackage, device, iface)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        // None of the already connected devices was suitable, waiting for new device
        while (true) {
            val device = waitForNewUsbDevice()
            val iface = getCtapHidInterface(device) ?: continue
            try {
                return handle(options, callerPackage, device, iface)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    companion object {
        const val TAG = "FidoUsbHandler"
        val CTAPHID_SIGNATURE = listOf<Byte>(0x06, 0xd0.toByte(), 0xf1.toByte(), 0x09, 0x01)
    }
}
