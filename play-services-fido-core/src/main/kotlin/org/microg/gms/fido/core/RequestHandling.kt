/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core

import android.annotation.TargetApi
import android.content.Context
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.fido.fido2.api.common.ErrorCode.*
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.microg.gms.fido.core.RequestOptionsType.REGISTER
import org.microg.gms.fido.core.RequestOptionsType.SIGN
import org.microg.gms.utils.getApplicationLabel
import org.microg.gms.utils.getFirstSignatureDigest
import org.microg.gms.utils.toBase64
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.or

class RequestHandlingException(val errorCode: ErrorCode, message: String? = null) : Exception(message)

enum class RequestOptionsType { REGISTER, SIGN }

val RequestOptions.registerOptions: PublicKeyCredentialCreationOptions
    get() = when (this) {
        is BrowserPublicKeyCredentialCreationOptions -> publicKeyCredentialCreationOptions
        is PublicKeyCredentialCreationOptions -> this
        else -> throw RequestHandlingException(DATA_ERR, "The request options are not valid")
    }

val RequestOptions.signOptions: PublicKeyCredentialRequestOptions
    get() = when (this) {
        is BrowserPublicKeyCredentialRequestOptions -> publicKeyCredentialRequestOptions
        is PublicKeyCredentialRequestOptions -> this
        else -> throw RequestHandlingException(DATA_ERR, "The request options are not valid")
    }

val RequestOptions.type: RequestOptionsType
    get() = when (this) {
        is PublicKeyCredentialCreationOptions, is BrowserPublicKeyCredentialCreationOptions -> REGISTER
        is PublicKeyCredentialRequestOptions, is BrowserPublicKeyCredentialRequestOptions -> RequestOptionsType.SIGN
        else -> throw RequestHandlingException(INVALID_STATE_ERR)
    }

val RequestOptions.webAuthnType: String
    get() = when (type) {
        REGISTER -> "webauthn.create"
        SIGN -> "webauthn.get"
    }

val RequestOptions.challenge: ByteArray
    get() = when (type) {
        REGISTER -> registerOptions.challenge
        SIGN -> signOptions.challenge
    }

val RequestOptions.rpId: String
    get() = when (type) {
        REGISTER -> registerOptions.rp.id
        SIGN -> signOptions.rpId
    }

fun RequestOptions.checkIsValid(context: Context, callingPackage: String) {
    if (type == REGISTER) {
        if (registerOptions.authenticatorSelection.requireResidentKey == true) {
            throw RequestHandlingException(
                NOT_SUPPORTED_ERR,
                "Resident credentials or empty 'allowCredentials' lists are not supported  at this time."
            )
        }
    }
    if (type == SIGN) {
        if (signOptions.allowList.isNullOrEmpty()) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "Request doesn't have a valid list of allowed credentials.")
        }
    }
    if (this is BrowserRequestOptions) {
        // TODO: Check properly if package is allowed to act as browser
        if (callingPackage != "com.android.chrome") {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "Not a browser.")
        }
    }
}

fun RequestOptions.getWebAuthnClientData(callingPackage: String, origin: String? = null): ByteArray {
    val obj = JSONObject()
        .put("type", webAuthnType)
        .put("challenge", challenge.toBase64(Base64.NO_PADDING, Base64.NO_WRAP, Base64.URL_SAFE))
        .put("androidPackageName", callingPackage)
        .put("tokenBinding", tokenBinding?.toJsonObject())
    if (origin != null) {
        obj.put("origin", origin)
    } else if (this is BrowserRequestOptions) {
        obj.put("origin", this.origin.toString())
    }
    return obj.toString().encodeToByteArray()
}

fun getApplicationName(context: Context, options: RequestOptions, callingPackage: String): String = when (options) {
    is BrowserPublicKeyCredentialCreationOptions, is BrowserPublicKeyCredentialRequestOptions -> options.rpId
    else -> context.packageManager.getApplicationLabel(callingPackage).toString()
}

fun getFacetId(context: Context, options: RequestOptions, callingPackage: String): String = when {
    options is BrowserRequestOptions -> {
        if (options.origin.scheme == null || options.origin.authority == null) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "Bad url ${options.origin}")
        }
        "${options.origin.scheme}://${options.origin.authority}"
    }
    else -> {
        val digest = context.packageManager.getFirstSignatureDigest(callingPackage, "SHA-256")
            ?: throw RequestHandlingException(NOT_ALLOWED_ERR, "Unknown package $callingPackage")
        "android:apk-key-hash:${digest.toBase64(Base64.NO_PADDING, Base64.NO_WRAP, Base64.URL_SAFE)}"
    }
}

class AttestedCredentialData(val aaguid: ByteArray, val id: ByteArray, val publicKey: ByteArray) {
    fun encode() = ByteBuffer.allocate(aaguid.size + 2 + id.size + publicKey.size)
        .put(aaguid)
        .order(ByteOrder.BIG_ENDIAN).putShort(id.size.toShort())
        .put(id)
        .put(publicKey)
        .array()
}


class AuthenticatorData(
    val rpIdHash: ByteArray,
    val userPresent: Boolean,
    val userVerified: Boolean,
    val signCount: Int,
    val attestedCredentialData: AttestedCredentialData? = null,
    val extensions: ByteArray? = null
) {
    fun encode(): ByteArray {
        val attestedCredentialData = attestedCredentialData?.encode() ?: ByteArray(0)
        val extensions = extensions ?: ByteArray(0)
        return ByteBuffer.allocate(rpIdHash.size + 5 + attestedCredentialData.size + extensions.size)
            .put(rpIdHash)
            .put(buildFlags(userPresent, userVerified, attestedCredentialData.isNotEmpty(), extensions.isNotEmpty()))
            .order(ByteOrder.BIG_ENDIAN).putInt(signCount)
            .put(attestedCredentialData)
            .put(extensions)
            .array()
    }

    fun toCBOR(): CBORObject = encode().toCBOR()

    companion object {
        /** User Present **/
        private const val FLAG_UP: Byte = 1

        /** User Verified **/
        private const val FLAG_UV: Byte = 4

        /** Attested credential data included **/
        private const val FLAG_AT: Byte = 64

        /** Extension data included **/
        private const val FLAG_ED: Byte = -128

        private fun buildFlags(up: Boolean, uv: Boolean, at: Boolean, ed: Boolean): Byte =
            (if (up) FLAG_UP else 0) or (if (uv) FLAG_UV else 0) or (if (at) FLAG_AT else 0) or (if (ed) FLAG_ED else 0)
    }
}

fun String.toCBOR() = CBORObject.FromObject(this)
fun ByteArray.toCBOR() = CBORObject.FromObject(this)
fun Int.toCBOR() = CBORObject.FromObject(this)

abstract class AttestationObject(val authData: AuthenticatorData) {
    abstract val fmt: String
    abstract val attStmt: CBORObject

    fun encode(): ByteArray = CBORObject.NewMap().apply {
        set("fmt", fmt.toCBOR())
        set("attStmt", attStmt)
        set("authData", authData.toCBOR())
    }.EncodeToBytes()
}

class NoneAttestationObject(authData: AuthenticatorData) : AttestationObject(authData) {
    override val fmt: String
        get() = "none"
    override val attStmt: CBORObject
        get() = CBORObject.NewMap()
}

class AndroidSafetyNetAttestationObject(authData: AuthenticatorData, val ver: String, val response: ByteArray) :
    AttestationObject(authData) {
    override val fmt: String
        get() = "android-safetynet"
    override val attStmt: CBORObject
        get() = CBORObject.NewMap().apply {
            set("ver", ver.toCBOR())
            set("response", response.toCBOR())
        }
}

class CoseKey(
    val algorithm: Algorithm,
    val x: BigInteger,
    val y: BigInteger,
    val curveId: Int,
    val curvePointSize: Int
) {
    fun encode(): ByteArray = CBORObject.NewMap().apply {
        set(1, 2.toCBOR())
        set(3, algorithm.algoValue.toCBOR())
        set(-1, curveId.toCBOR())
        set(-2, x.toByteArray(curvePointSize).toCBOR())
        set(-3, y.toByteArray(curvePointSize).toCBOR())
    }.EncodeToBytes()

    companion object {
        fun BigInteger.toByteArray(size: Int): ByteArray {
            val res = ByteArray(size)
            val orig = toByteArray()
            if (orig.size > size) {
                System.arraycopy(orig, orig.size - size, res, 0, size)
            } else {
                System.arraycopy(orig, 0, res, size - orig.size, orig.size)
            }
            return res
        }
    }
}

class CredentialId(val type: Byte, val data: ByteArray, val rpId: String, val publicKey: PublicKey) {
    fun encode(): ByteArray = ByteBuffer.allocate(1 + data.size + 32).apply {
        put(type)
        put(data)
        put((rpId.toByteArray() + publicKey.encoded).digest("SHA-256"))
    }.array()

    companion object {
        fun decodeTypeAndData(bytes: ByteArray): Pair<Byte, ByteArray> {
            val buffer = ByteBuffer.wrap(bytes)
            val type = buffer.get()
            val data = ByteArray(32)
            buffer.get(data)
            return type to data
        }
    }
}

fun ByteArray.digest(md: String): ByteArray = MessageDigest.getInstance(md).digest(this)

fun getClientDataAndHash(options: RequestOptions, callingPackage: String): Pair<ByteArray, ByteArray> {
    val clientData: ByteArray?
    var clientDataHash = (options as? BrowserPublicKeyCredentialCreationOptions)?.clientDataHash
    if (clientDataHash == null) {
        clientData = options.getWebAuthnClientData(callingPackage)
        clientDataHash = clientData.digest("SHA-256")
    } else {
        clientData = "<invalid>".toByteArray()
    }
    return clientData to clientDataHash
}

@TargetApi(23)
suspend fun getActiveSignature(
    activity: FragmentActivity,
    options: RequestOptions,
    callingPackage: String,
    store: InternalCredentialStore,
    keyId: ByteArray
): Signature {
    val signature = store.getSignature(options.rpId, keyId) ?: throw RequestHandlingException(INVALID_STATE_ERR)
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
                continuation.resumeWithException(RequestHandlingException(NOT_ALLOWED_ERR, errorMessage))
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.fido_biometric_prompt_title))
                .setDescription(
                    activity.getString(
                        R.string.fido_biometric_prompt_body,
                        getApplicationName(activity, options, callingPackage)
                    )
                )
                .setNegativeButtonText(activity.getString(android.R.string.cancel))
                .build(),
            BiometricPrompt.CryptoObject(signature)
        )
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }
    return signature
}

@RequiresApi(23)
suspend fun registerInternal(
    activity: FragmentActivity,
    options: RequestOptions,
    callingPackage: String
): AuthenticatorAttestationResponse {
    if (options.type != REGISTER) throw RequestHandlingException(INVALID_STATE_ERR)
    val store = InternalCredentialStore(activity)
    // TODO: Privacy?
    for (descriptor in options.registerOptions.excludeList.orEmpty()) {
        if (store.containsKey(options.rpId, descriptor.id)) {
            throw RequestHandlingException(
                NOT_ALLOWED_ERR,
                "An excluded credential has already been registered with the device"
            )
        }
    }
    val (clientData, clientDataHash) = getClientDataAndHash(options, callingPackage)
    if (options.registerOptions.attestationConveyancePreference in setOf(AttestationConveyancePreference.NONE, null)) {
        // No attestation needed
    } else {
        // TODO: SafetyNet
        throw RequestHandlingException(NOT_SUPPORTED_ERR, "SafetyNet Attestation not yet supported")
    }
    val keyId = store.createKey(options.rpId)
    val publicKey = store.getPublicKey(options.rpId, keyId) ?: throw RequestHandlingException(INVALID_STATE_ERR)

    // We're ignoring the signature object as we don't need it for registration
    getActiveSignature(activity, options, callingPackage, store, keyId)

    val (x, y) = (publicKey as ECPublicKey).w.let { it.affineX to it.affineY }
    val coseKey = CoseKey(EC2Algorithm.ES256, x, y, 1, 32)
    val credentialId = CredentialId(1, keyId, options.rpId, publicKey)

    val credentialData = AttestedCredentialData(
        ByteArray(16), // 0xb93fd961f2e6462fb12282002247de78 for SafetyNet
        credentialId.encode(),
        coseKey.encode()
    )

    val authenticatorData = AuthenticatorData(
        options.rpId.toByteArray().digest("SHA-256"),
        userPresent = true,
        userVerified = true,
        signCount = 0,
        attestedCredentialData = credentialData
    )

    return AuthenticatorAttestationResponse(
        credentialId.encode(),
        clientData,
        NoneAttestationObject(authenticatorData).encode()
    )
}

@RequiresApi(23)
suspend fun signInternal(
    activity: FragmentActivity,
    options: RequestOptions,
    callingPackage: String
): AuthenticatorAssertionResponse {
    if (options.type != SIGN) throw RequestHandlingException(INVALID_STATE_ERR)
    val store = InternalCredentialStore(activity)
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
        // TODO: Privacy
        throw RequestHandlingException(
            NOT_ALLOWED_ERR,
            "Cannot find credential in local KeyStore or database"
        )
    }

    val (clientData, clientDataHash) = getClientDataAndHash(options, callingPackage)

    val credentialId = candidates.first()
    val keyId = credentialId.data

    val (x, y) = (credentialId.publicKey as ECPublicKey).w.let { it.affineX to it.affineY }
    val coseKey = CoseKey(EC2Algorithm.ES256, x, y, 1, 32)

    val credentialData = AttestedCredentialData(
        ByteArray(16), // 0xb93fd961f2e6462fb12282002247de78 for SafetyNet
        credentialId.encode(),
        coseKey.encode()
    )

    val authenticatorData = AuthenticatorData(
        options.rpId.toByteArray().digest("SHA-256"),
        userPresent = true,
        userVerified = true,
        signCount = 0,
        attestedCredentialData = credentialData
    )

    val signature = getActiveSignature(activity, options, callingPackage, store, keyId)

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
