/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.screenlock

import android.app.KeyguardManager
import android.os.Build.VERSION.SDK_INT
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.microg.gms.common.Constants
import org.microg.gms.fido.core.AuthenticatorResponseWrapper
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.RequestOptionsType
import org.microg.gms.fido.core.UserInfo
import org.microg.gms.fido.core.digest
import org.microg.gms.fido.core.getApplicationName
import org.microg.gms.fido.core.getClientDataAndHash
import org.microg.gms.fido.core.protocol.AndroidKeyAttestationObject
import org.microg.gms.fido.core.protocol.AndroidSafetyNetAttestationObject
import org.microg.gms.fido.core.protocol.AttestedCredentialData
import org.microg.gms.fido.core.protocol.AuthenticatorData
import org.microg.gms.fido.core.protocol.CoseKey
import org.microg.gms.fido.core.protocol.CredentialId
import org.microg.gms.fido.core.protocol.NoneAttestationObject
import org.microg.gms.fido.core.registerOptions
import org.microg.gms.fido.core.rpId
import org.microg.gms.fido.core.signOptions
import org.microg.gms.fido.core.skipAttestation
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.type
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
    ): suspend () -> AuthenticatorAttestationResponse {
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
            store.getPublicKey(options.rpId, keyId)
                ?: throw RequestHandlingException(ErrorCode.INVALID_STATE_ERR)

        val name = options.registerOptions.user.name
        val displayName = options.registerOptions.user.displayName
        val icon = options.registerOptions.user.icon

        store.addUserInfo(options.rpId, keyId, name, displayName, icon)

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

        val response = AuthenticatorAttestationResponse(
            credentialId.encode(),
            clientData,
            attestationObject.encode(),
            arrayOf("internal")
        )

        return suspend { response }
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
    ): Pair<List<Pair<UserInfo?,suspend () -> AuthenticatorAssertionResponse>>, List<suspend () -> Boolean>> {
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

        // If there is no allowlist, add all keys with the given rpId as possible keys
        if (options.signOptions.allowList?.isEmpty() != false) {
            val keys = store.getPublicKeys(options.rpId)
            for ((alias, key) in keys) {
                val aliasSplit = alias.split(Regex("\\."), 3)
                if (aliasSplit.size != 3) continue
                val type: Int = aliasSplit[0].toIntOrNull() ?: continue
                if (type != 1) continue

                val data: ByteArray
                try {
                     data = Base64.decode(aliasSplit[1], Base64.DEFAULT)
                } catch (e: Exception) {
                    continue
                }

                candidates.add(CredentialId(type.toByte(), data, options.rpId, key))
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

        val credentialList = ArrayList<Pair<UserInfo?, suspend () -> AuthenticatorAssertionResponse>>()
        val deleteFunctions = ArrayList<suspend () -> Boolean>()

        for (credentialId in candidates) {
            val keyId = credentialId.data
            val authenticatorData = getAuthenticatorData(options.rpId, null)
            val userInfo: UserInfo? = store.getUserInfo(options.rpId, keyId)

            val responseCallable = suspend {
                val signature = getActiveSignature(options, callerPackage, keyId)
                signature.update(authenticatorData.encode() + clientDataHash)
                val sig = signature.sign()

                AuthenticatorAssertionResponse(
                    credentialId.encode(),
                    clientData,
                    authenticatorData.encode(),
                    sig,
                    null
                )
            }

            val deleteFunction = suspend {
                try {
                    showBiometricPrompt(getApplicationName(activity, options, callerPackage), null)
                    store.deleteKey(options.rpId, keyId)
                    true
                } catch (e: RequestHandlingException) {
                    false
                }
            }

            credentialList.add(userInfo to responseCallable)
            deleteFunctions.add(deleteFunction)
        }

        return credentialList to deleteFunctions
    }

    @RequiresApi(24)
    override suspend fun start(options: RequestOptions, callerPackage: String, pinRequested: Boolean, pin: String?): AuthenticatorResponseWrapper =
        when (options.type) {
            RequestOptionsType.REGISTER -> AuthenticatorResponseWrapper(listOf(Pair(null, register(options, callerPackage))))
            RequestOptionsType.SIGN -> {
                val (responseChoices, deleteFunctions) = sign(options, callerPackage)
                AuthenticatorResponseWrapper(responseChoices, deleteFunctions)
            }
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
