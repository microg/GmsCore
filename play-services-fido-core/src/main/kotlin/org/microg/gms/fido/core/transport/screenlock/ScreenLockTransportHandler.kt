/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.screenlock

import android.app.KeyguardManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.fido.fido2.api.common.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.protocol.*
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import java.security.Signature
import java.security.interfaces.ECPublicKey
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenLockTransportHandler(private val activity: FragmentActivity, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.SCREEN_LOCK, callback) {
    private val store by lazy { ScreenLockCredentialStore(activity) }

    override val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= 23 && activity.getSystemService<KeyguardManager>()?.isDeviceSecure == true

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

    fun getCredentialData(credentialId: CredentialId, coseKey: CoseKey) = AttestedCredentialData(
        ByteArray(16), // 0xb93fd961f2e6462fb12282002247de78 for SafetyNet
        credentialId.encode(),
        coseKey.encode()
    )

    fun getAuthenticatorData(
        rpId: String,
        credentialData: AttestedCredentialData,
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

    @RequiresApi(23)
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
        if (options.registerOptions.attestationConveyancePreference in setOf(
                AttestationConveyancePreference.NONE,
                null
            )
        ) {
            // No attestation needed
        } else {
            // TODO: SafetyNet
            throw RequestHandlingException(ErrorCode.NOT_SUPPORTED_ERR, "SafetyNet Attestation not yet supported")
        }
        val keyId = store.createKey(options.rpId)
        val publicKey =
            store.getPublicKey(options.rpId, keyId) ?: throw RequestHandlingException(ErrorCode.INVALID_STATE_ERR)

        // We're ignoring the signature object as we don't need it for registration
        getActiveSignature(options, callerPackage, keyId)

        val (x, y) = (publicKey as ECPublicKey).w.let { it.affineX to it.affineY }
        val coseKey = CoseKey(EC2Algorithm.ES256, x, y, 1, 32)
        val credentialId = CredentialId(1, keyId, options.rpId, publicKey)

        val credentialData = getCredentialData(credentialId, coseKey)
        val authenticatorData = getAuthenticatorData(options.rpId, credentialData)

        return AuthenticatorAttestationResponse(
            credentialId.encode(),
            clientData,
            NoneAttestationObject(authenticatorData).encode()
        )
    }

    suspend fun sign(
        options: RequestOptions,
        callerPackage: String
    ): AuthenticatorAssertionResponse {
        if (options.type != RequestOptionsType.SIGN) throw RequestHandlingException(ErrorCode.INVALID_STATE_ERR)
        val candidates = mutableListOf<CredentialId>()
        for (descriptor in options.signOptions.allowList) {
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

        val (x, y) = (credentialId.publicKey as ECPublicKey).w.let { it.affineX to it.affineY }
        val coseKey = CoseKey(EC2Algorithm.ES256, x, y, 1, 32)

        val credentialData = getCredentialData(credentialId, coseKey)
        val authenticatorData = getAuthenticatorData(options.rpId, credentialData)

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

    @RequiresApi(23)
    override suspend fun start(options: RequestOptions, callerPackage: String): AuthenticatorResponse =
        when (options.type) {
            RequestOptionsType.REGISTER -> register(options, callerPackage)
            RequestOptionsType.SIGN -> sign(options, callerPackage)
        }

    override fun shouldBeUsedInstantly(options: RequestOptions): Boolean {
        if (options.type != RequestOptionsType.SIGN) return false
        for (descriptor in options.signOptions.allowList) {
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
}
