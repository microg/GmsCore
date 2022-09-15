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
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.fido.fido2.api.common.*
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.protocol.*
import org.microg.gms.fido.core.protocol.msgs.*
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidConnection
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidMessageStatusException
import org.microg.gms.utils.toBase64

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
                    Log.d(TAG, "Failed claiming interface")
                    false
                }
            } == true
            if (match) {
                return iface
            } else {
                Log.d(TAG, "${device.productName} signature does not match")
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
        val (response, clientData, keyHandle) = CtapHidConnection(context, device, iface).open {
            val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
            if (it.hasCtap2Support) {
                val reqOptions = AuthenticatorMakeCredentialRequest.Companion.Options(
                    options.registerOptions.authenticatorSelection?.requireResidentKey,
                    options.registerOptions.authenticatorSelection?.requireUserVerification?.let { it == UserVerificationRequirement.REQUIRED })
                val extensions = mutableMapOf<String, CBORObject>()
                if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
                    extensions["appidExclude"] =
                        options.authenticationExtensions.fidoAppIdExtension.appId.encodeAsCbor()
                }
                if (options.authenticationExtensions?.userVerificationMethodExtension?.uvm != null) {
                    extensions["uvm"] =
                        options.authenticationExtensions.userVerificationMethodExtension.uvm.encodeAsCbor()
                }
                val request = AuthenticatorMakeCredentialRequest(
                    clientDataHash,
                    options.registerOptions.rp,
                    options.registerOptions.user,
                    options.registerOptions.parameters,
                    options.registerOptions.excludeList,
                    extensions,
                    reqOptions
                )
                val ctap2Response = it.runCommand(AuthenticatorMakeCredentialCommand(request))
                val credentialId = AuthenticatorData.decode(ctap2Response.authData).attestedCredentialData?.id
                return@open Triple(ctap2Response, clientData, credentialId)
            }
            if (it.hasCtap1Support) {
                val rpIdHash = options.rpId.toByteArray().digest("SHA-256")
                val appIdHash =
                    options.authenticationExtensions?.fidoAppIdExtension?.appId?.toByteArray()?.digest("SHA-256")
                if (!options.registerOptions.parameters.isNullOrEmpty() && options.registerOptions.parameters.all { it.algorithmIdAsInteger != -7 }) throw IllegalArgumentException(
                    "Can't use CTAP1 protocol for non ES256 requests"
                )
                if (options.registerOptions.authenticatorSelection.requireResidentKey == true) throw IllegalArgumentException(
                    "Can't use CTAP1 protocol when resident key required"
                )
                val hasCredential = options.registerOptions.excludeList.any { cred ->
                    deviceHasCredential(it, clientDataHash, rpIdHash, cred) ||
                            if (appIdHash != null) {
                                deviceHasCredential(it, clientDataHash, appIdHash, cred)
                            } else {
                                false
                            }
                }
                while (true) {
                    try {
                        val response = it.runCommand(U2fRegistrationCommand(clientDataHash, rpIdHash))
                        if (hasCredential) throw RequestHandlingException(
                            ErrorCode.NOT_ALLOWED_ERR,
                            "An excluded credential has already been registered with the device"
                        )
                        require(response.userPublicKey[0] == 0x04.toByte())
                        val coseKey = CoseKey(
                            EC2Algorithm.ES256,
                            response.userPublicKey.sliceArray(1 until 33),
                            response.userPublicKey.sliceArray(33 until 65),
                            1
                        )
                        val credentialData =
                            AttestedCredentialData(ByteArray(16), response.keyHandle, coseKey.encode())
                        val authData = AuthenticatorData(
                            options.rpId.toByteArray().digest("SHA-256"),
                            true,
                            false,
                            0,
                            credentialData
                        )
                        val attestationObject =
                            FidoU2fAttestationObject(authData, response.signature, response.attestationCertificate)
                        val ctap2Response = AuthenticatorMakeCredentialResponse(
                            authData.encode(),
                            attestationObject.fmt,
                            attestationObject.attStmt
                        )
                        return@open Triple(ctap2Response, clientData, response.keyHandle)
                    } catch (e: CtapHidMessageStatusException) {
                        if (e.status != 0x6985) {
                            throw e
                        }
                    }
                    delay(100)
                }
            }
            throw IllegalStateException()
        }
        return AuthenticatorAttestationResponse(
            keyHandle,
            clientData,
            AnyAttestationObject(response.authData, response.fmt, response.attStmt).encode()
        )
    }

    suspend fun ctap1sign(
        connection: CtapHidConnection,
        options: RequestOptions,
        clientData: ByteArray,
        clientDataHash: ByteArray,
        rpIdHash: ByteArray
    ): Triple<AuthenticatorGetAssertionResponse, ByteArray, ByteArray> {
        val cred = options.signOptions.allowList.firstOrNull { cred ->
            deviceHasCredential(connection, clientDataHash, rpIdHash, cred)
        } ?: options.signOptions.allowList.first()

        while (true) {
            try {
                val response = connection.runCommand(U2fAuthenticationCommand(0x03, clientDataHash, rpIdHash, cred.id))
                val authData = AuthenticatorData(rpIdHash, response.userPresence, false, response.counter)
                val ctap2Response = AuthenticatorGetAssertionResponse(
                    cred,
                    authData.encode(),
                    response.signature,
                    null,
                    null
                )
                return Triple(ctap2Response, clientData, cred.id)
            } catch (e: CtapHidMessageStatusException) {
                if (e.status != 0x6985) {
                    throw e
                }
                delay(100)
            }
        }
    }

    suspend fun sign(
        options: RequestOptions,
        callerPackage: String,
        device: UsbDevice,
        iface: UsbInterface
    ): AuthenticatorAssertionResponse {
        val (response, clientData, credentialId) = CtapHidConnection(context, device, iface).open {
            val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
            if (it.hasCtap2Support) {
                val reqOptions = AuthenticatorGetAssertionRequest.Companion.Options(
                    userVerification = options.signOptions.requireUserVerification?.let { it == UserVerificationRequirement.REQUIRED }
                )
                val extensions = mutableMapOf<String, CBORObject>()
                if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
                    extensions["appid"] = options.authenticationExtensions.fidoAppIdExtension.appId.encodeAsCbor()
                }
                if (options.authenticationExtensions?.userVerificationMethodExtension?.uvm != null) {
                    extensions["uvm"] =
                        options.authenticationExtensions.userVerificationMethodExtension.uvm.encodeAsCbor()
                }
                val request = AuthenticatorGetAssertionRequest(
                    options.rpId,
                    clientDataHash,
                    options.signOptions.allowList,
                    extensions,
                    reqOptions
                )
                val ctap2Response = it.runCommand(AuthenticatorGetAssertionCommand(request))
                Log.d(TAG, "Authenticator data: ${ctap2Response.authData.toBase64(Base64.NO_WRAP)}")
                return@open Triple(ctap2Response, clientData, ctap2Response.credential?.id)
            }
            if (it.hasCtap1Support) {
                try {
                    val rpIdHash = options.rpId.toByteArray().digest("SHA-256")
                    return@open ctap1sign(it, options, clientData, clientDataHash, rpIdHash)
                } catch (e: Exception) {
                    try {
                        if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
                            val appIdHash = options.authenticationExtensions.fidoAppIdExtension.appId.toByteArray()
                                .digest("SHA-256")
                            return@open ctap1sign(it, options, clientData, clientDataHash, appIdHash)
                        }
                    } catch (e2: Exception) {
                    }
                    // Throw original
                    throw e
                }
            }
            throw IllegalStateException()
        }

        return AuthenticatorAssertionResponse(
            credentialId,
            clientData,
            response.authData,
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
        Log.d(TAG, "Trying to use ${device.productName} for ${options.type}")
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
