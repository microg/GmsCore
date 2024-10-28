/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.screenlock

import android.app.KeyguardManager
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.microg.gms.common.Constants
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.protocol.*
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import java.security.Signature
import java.security.interfaces.ECPublicKey
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RequiresApi(23)
class ScreenLockTransportHandler(private val activity: FragmentActivity, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.SCREEN_LOCK, callback) {
    private val store by lazy { ScreenLockCredentialStore(activity) }

    override val isSupported: Boolean
        get() = activity.getSystemService<KeyguardManager>()?.isDeviceSecure == true

    suspend fun showBiometricPrompt(applicationName: String, signature: Signature?) {
        suspendCancellableCoroutine<BiometricPrompt.AuthenticationResult> { continuation ->
            val prompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val errorMessage = when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "User canceled verification"
                        else -> errString.toString()
                    }
                    continuation.resumeWithException(RequestHandlingException(ErrorCode.NOT_ALLOWED_ERR, errorMessage))
                }
            })
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.fido_biometric_prompt_title))
                .setDescription(
                    activity.getString(
                        R.string.fido_biometric_prompt_body,
                        applicationName
                    )
                )
                .setNegativeButtonText(activity.getString(android.R.string.cancel))
                .build()
            invokeStatusChanged(TransportHandlerCallback.STATUS_WAITING_FOR_USER)
            if (signature != null) {
                prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(signature))
            } else {
                prompt.authenticate(promptInfo)
            }
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }

    suspend fun getActiveSignature(
        options: RequestOptions,
        callingPackage: String,
        keyId: ByteArray
    ): Signature {
        val signature =
            store.getSignature(options.rpId, keyId) ?: throw RequestHandlingException(ErrorCode.INVALID_STATE_ERR)
        showBiometricPrompt(getApplicationName(activity, options, callingPackage), signature)
        return signature
    }

    fun getCredentialData(aaguid: ByteArray, credentialId: CredentialId, coseKey: CoseKey) = AttestedCredentialData(
        aaguid,
        credentialId.encode(),
        coseKey.encode()
    )

    fun getAuthenticatorData(
        rpId: String,
        credentialData: AttestedCredentialData?,
        userPresent: Boolean = true,
        userVerified: Boolean = true,
        signCount: Int = 0
    ) = AuthenticatorData(
        rpId.toByteArray().digest("SHA-256"),
        userPresent = userPresent,
        userVerified = userVerified,
        signCount = signCount,
        attestedCredentialData = credentialData
    )

    suspend fun register(
        options: RequestOptions,
        callerPackage: String
    ): AuthenticatorAttestationResponse {
        if (options.type != RequestOptionsType.REGISTER) throw RequestHandlingException(ErrorCode.INVALID_STATE_ERR)
        for (descriptor in options.registerOptions.excludeList.orEmpty()) {
            if (store.containsKey(options.rpId, descriptor.id)) {
                throw RequestHandlingException(
                    ErrorCode.NOT_ALLOWED_ERR,
                    "An excluded credential has already been registered with the device"
                )
            }
        }
        val (clientData, clientDataHash) = getClientDataAndHash(activity, options, callerPackage)
        val aaguid = if (options.registerOptions.skipAttestation) ByteArray(16) else AAGUID
        val keyId = store.createKey(options.rpId, clientDataHash)
        val publicKey =
            store.getPublicKey(options.rpId, keyId) ?: throw RequestHandlingException(ErrorCode.INVALID_STATE_ERR)

        // We're ignoring the signature object as we don't need it for registration
        val signature = getActiveSignature(options, callerPackage, keyId)

        val (x, y) = (publicKey as ECPublicKey).w.let { it.affineX to it.affineY }
        val coseKey = CoseKey(EC2Algorithm.ES256, x, y, 1, 32)
        val credentialId = CredentialId(1, keyId, options.rpId, publicKey)

        val credentialData = getCredentialData(aaguid, credentialId, coseKey)
        val authenticatorData = getAuthenticatorData(options.rpId, credentialData)

        val attestationObject = if (options.registerOptions.skipAttestation) {
            NoneAttestationObject(authenticatorData)
        } else {
            try {
                if (SDK_INT >= 24) {
                    createAndroidKeyAttestation(signature, authenticatorData, clientDataHash, options.rpId, keyId)
                } else {
                    createSafetyNetAttestation(authenticatorData, clientDataHash)
                }
            } catch (e: Exception) {
                Log.w("FidoScreenLockTransport", e)
                NoneAttestationObject(authenticatorData)
            }
        }

        return AuthenticatorAttestationResponse(
            credentialId.encode(),
            clientData,
            attestationObject.encode(),
            arrayOf("internal")
        )
    }

    @RequiresApi(24)
    private fun createAndroidKeyAttestation(
        signature: Signature,
        authenticatorData: AuthenticatorData,
        clientDataHash: ByteArray,
        rpId: String,
        keyId: ByteArray
    ): AndroidKeyAttestationObject {
        signature.update(authenticatorData.encode() + clientDataHash)
        val sig = signature.sign()
        return AndroidKeyAttestationObject(
            authenticatorData,
            EC2Algorithm.ES256,
            sig,
            store.getCertificateChain(rpId, keyId).map { it.encoded })
    }

    private suspend fun createSafetyNetAttestation(
        authenticatorData: AuthenticatorData,
        clientDataHash: ByteArray
    ): AndroidSafetyNetAttestationObject {
        val response = SafetyNet.getClient(activity).attest(
            (authenticatorData.encode() + clientDataHash).digest("SHA-256"),
            "AIzaSyDqVnJBjE5ymo--oBJt3On7HQx9xNm1RHA"
        ).await()
        return AndroidSafetyNetAttestationObject(
            authenticatorData,
            Constants.GMS_VERSION_CODE.toString(),
            response.jwsResult.toByteArray()
        )
    }

    suspend fun sign(
        options: RequestOptions,
        callerPackage: String
    ): AuthenticatorAssertionResponse {
        if (options.type != RequestOptionsType.SIGN) throw RequestHandlingException(ErrorCode.INVALID_STATE_ERR)
        val candidates = mutableListOf<CredentialId>()
        for (descriptor in options.signOptions.allowList.orEmpty()) {
            try {
                val (type, data) = CredentialId.decodeTypeAndData(descriptor.id)
                if (type == 1.toByte() && store.containsKey(options.rpId, data)) {
                    candidates.add(CredentialId(type, data, options.rpId, store.getPublicKey(options.rpId, data)!!))
                }
            } catch (e: Exception) {
                // Not in store or unknown id
            }
        }
        if (candidates.isEmpty()) {
            // Show a biometric prompt even if no matching key to effectively rate-limit
            showBiometricPrompt(getApplicationName(activity, options, callerPackage), null)
            throw RequestHandlingException(
                ErrorCode.NOT_ALLOWED_ERR,
                "Cannot find credential in local KeyStore or database"
            )
        }

        val (clientData, clientDataHash) = getClientDataAndHash(activity, options, callerPackage)

        val credentialId = candidates.first()
        val keyId = credentialId.data
        val authenticatorData = getAuthenticatorData(options.rpId, null)

        val signature = getActiveSignature(options, callerPackage, keyId)
        signature.update(authenticatorData.encode() + clientDataHash)
        val sig = signature.sign()

        return AuthenticatorAssertionResponse(
            credentialId.encode(),
            clientData,
            authenticatorData.encode(),
            sig,
            null
        )
    }

    @RequiresApi(24)
    override suspend fun start(options: RequestOptions, callerPackage: String, pinRequested: Boolean, pin: String?): AuthenticatorResponse =
        when (options.type) {
            RequestOptionsType.REGISTER -> register(options, callerPackage)
            RequestOptionsType.SIGN -> sign(options, callerPackage)
        }

    override fun shouldBeUsedInstantly(options: RequestOptions): Boolean {
        if (options.type != RequestOptionsType.SIGN) return false
        for (descriptor in options.signOptions.allowList.orEmpty()) {
            try {
                val (type, data) = CredentialId.decodeTypeAndData(descriptor.id)
                if (type == 1.toByte() && store.containsKey(options.rpId, data)) {
                    return true
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return false
    }

    companion object {
        private val AAGUID = byteArrayOf(
            0xb9.toByte(), 0x3f, 0xd9.toByte(), 0x61, 0xf2.toByte(), 0xe6.toByte(), 0x46, 0x2f,
            0xb1.toByte(), 0x22, 0x82.toByte(), 0x00, 0x22, 0x47, 0xde.toByte(), 0x78
        )
    }
}
