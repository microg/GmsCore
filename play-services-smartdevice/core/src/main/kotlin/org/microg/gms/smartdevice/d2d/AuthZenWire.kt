/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.util.Base64
import com.google.security.cryptauth.lib.securegcm.GcmMetadata
import com.google.security.cryptauth.lib.securegcm.IdentityAssertion
import com.google.security.cryptauth.lib.securegcm.Type as GcmMessageType
import com.google.security.cryptauth.lib.securemessage.EcP256PublicKey
import com.google.security.cryptauth.lib.securemessage.EncScheme
import com.google.security.cryptauth.lib.securemessage.GenericPublicKey
import com.google.security.cryptauth.lib.securemessage.Header
import com.google.security.cryptauth.lib.securemessage.HeaderAndBodyInternal
import com.google.security.cryptauth.lib.securemessage.PublicKeyType
import com.google.security.cryptauth.lib.securemessage.SecureMessage
import com.google.security.cryptauth.lib.securemessage.SigScheme
import okio.ByteString.Companion.toByteString
import org.json.JSONStringer
import org.microg.gms.smartdevice.d2d.proto.DeviceSignals
import org.microg.gms.smartdevice.d2d.proto.LockScreenQuality
import org.microg.gms.smartdevice.d2d.proto.UserPresence
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.microg.gms.utils.hkdfSha256

object AuthZenWire {
    private val secureMessagePrefix = sha256("SecureMessage".toByteArray(StandardCharsets.UTF_8))

    fun encodeGenericPublicKey(publicKey: ECPublicKey): ByteArray {
        val ecKey = EcP256PublicKey(
            x = publicKey.w.affineX.toByteArray().toByteString(),
            y = publicKey.w.affineY.toByteArray().toByteString(),
        )
        val genericKey = GenericPublicKey(
            type = PublicKeyType.EC_P256,
            ecP256PublicKey = ecKey,
        )
        return GenericPublicKey.ADAPTER.encode(genericKey)
    }

    fun decodeGenericPublicKey(encoded: ByteArray): ECPublicKey {
        val genericKey = GenericPublicKey.ADAPTER.decode(encoded)
        require(genericKey.type == PublicKeyType.EC_P256) { "CryptAuth DH key is not P-256" }
        val ecKey = requireNotNull(genericKey.ecP256PublicKey) { "CryptAuth DH key material is missing" }
        val parameters = AlgorithmParameters.getInstance("EC").run {
            init(ECGenParameterSpec("secp256r1"))
            getParameterSpec(ECParameterSpec::class.java)
        }
        return KeyFactory.getInstance("EC").generatePublic(
            ECPublicKeySpec(
                ECPoint(BigInteger(ecKey.x.toByteArray()), BigInteger(ecKey.y.toByteArray())),
                parameters,
            ),
        ) as ECPublicKey
    }

    fun encodeUserPresence(
        lockScreenSecure: Boolean,
        shownLockScreen: Boolean,
    ): ByteArray {
        val userPresence = UserPresence(
            lockScreenSecure = lockScreenSecure,
            shownLockScreen = shownLockScreen,
            lockScreenQuality = if (lockScreenSecure) {
                LockScreenQuality.LOCK_SCREEN_QUALITY_SECURE
            } else {
                LockScreenQuality.LOCK_SCREEN_QUALITY_NONE
            },
            lastUnlockDurationInSeconds = -1,
            lockScreenSetupDurationInSeconds = -1,
        )
        return UserPresence.ADAPTER.encode(userPresence)
    }

    fun encodeSourceDeviceSignals(
        androidId: String,
        device: String,
        model: String,
        sdkVersion: String,
        googlePlayServicesVersion: String,
        droidGuardResults: String,
        userPresence: ByteArray,
    ): ByteArray {
        val deviceSignals = DeviceSignals(
            deviceId = androidId,
            deviceName = device,
            deviceModel = model,
            sdkVersion = sdkVersion,
            googlePlayServicesVersion = googlePlayServicesVersion,
            droidGuardResults = droidGuardResults.takeIf { it.isNotEmpty() },
            userPresence = UserPresence.ADAPTER.decode(userPresence),
        )
        return DeviceSignals.ADAPTER.encode(deviceSignals)
    }

    fun createClientData(accountIdentifier: String, challenge: ByteArray, sourceDeviceSignals: ByteArray): ByteArray {
        val json = JSONStringer()
            .`object`()
            .key("typ").value("navigator.id.getAssertion")
            .key("challenge").value(base64Url(challenge))
            .key("source_device_signals").value(base64Url(sourceDeviceSignals))
            .key("account_identifier").value(accountIdentifier)
            .endObject()
            .toString()
        return json.toByteArray(StandardCharsets.UTF_8)
    }

    fun encodeIdentityAssertion(clientData: ByteArray, counter: Long): ByteArray {
        val assertion = IdentityAssertion(
            browserDataHash = sha256(clientData).toByteString(),
            counter = counter,
            userApproval = USER_APPROVAL_EXPLICIT,
        )
        return IdentityAssertion.ADAPTER.encode(assertion)
    }

    fun encodeIdentityAssertionMetadata(): ByteArray {
        val metadata = GcmMetadata(
            type = GcmMessageType.GCMV1_IDENTITY_ASSERTION,
            version = IDENTITY_ASSERTION_VERSION,
        )
        return GcmMetadata.ADAPTER.encode(metadata)
    }

    fun encodeSecureMessageHeader(
        metadata: ByteArray,
        iv: ByteArray,
        verificationKeyId: ByteArray,
    ): ByteArray {
        require(iv.size == AES_BLOCK_SIZE) { "SecureMessage AES-CBC IV must be 16 bytes" }
        val header = Header(
            signatureScheme = SigScheme.ECDSA_P256_SHA256,
            encryptionScheme = EncScheme.AES_256_CBC,
            verificationKeyId = verificationKeyId.toByteString(),
            iv = iv.toByteString(),
            publicMetadata = metadata.toByteString(),
        )
        return Header.ADAPTER.encode(header)
    }

    private fun secureMessageEncryptionPlaintext(header: ByteArray, body: ByteArray): ByteArray =
        sha256(header).copyOf(SECURE_MESSAGE_HEADER_DIGEST_LENGTH) + body

    private fun deriveSecureMessageEncryptionKey(authZenKey: ByteArray): ByteArray {
        require(authZenKey.size == 16 || authZenKey.size == 32) { "Invalid AuthZen AES key length" }
        return hkdfSha256(
            authZenKey,
            secureMessagePrefix,
            SECURE_MESSAGE_ENCRYPTION_INFO,
            SECURE_MESSAGE_AES_KEY_LENGTH,
        )
    }

    fun encryptSecureMessageBody(
        authZenKey: ByteArray,
        header: ByteArray,
        iv: ByteArray,
        body: ByteArray,
    ): ByteArray {
        require(iv.size == AES_BLOCK_SIZE) { "SecureMessage AES-CBC IV must be 16 bytes" }
        val plaintext = secureMessageEncryptionPlaintext(header, body)
        val encryptionKey = deriveSecureMessageEncryptionKey(authZenKey)
        return try {
            Cipher.getInstance(SECURE_MESSAGE_CIPHER).run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(iv))
                doFinal(plaintext)
            }
        } finally {
            encryptionKey.fill(0)
            plaintext.fill(0)
        }
    }

    fun encodeSecureMessageSignedData(header: ByteArray, ciphertext: ByteArray): ByteArray {
        val signedData = HeaderAndBodyInternal(
            header_ = header.toByteString(),
            body = ciphertext.toByteString(),
        )
        return HeaderAndBodyInternal.ADAPTER.encode(signedData)
    }

    fun secureMessageSignatureInput(headerAndBody: ByteArray): ByteArray =
        secureMessagePrefix + headerAndBody

    fun encodeSecureMessage(headerAndBody: ByteArray, signature: ByteArray): ByteArray {
        val secureMessage = SecureMessage(
            headerAndBody = headerAndBody.toByteString(),
            signature = signature.toByteString(),
        )
        return SecureMessage.ADAPTER.encode(secureMessage)
    }

    fun base64(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)

    fun base64Url(value: ByteArray): String =
        Base64.encodeToString(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    fun decodeBase64(value: String): ByteArray = try {

        Base64.decode(value, Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun sha256(value: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(value)

    private const val USER_APPROVAL_EXPLICIT = 1
    private const val IDENTITY_ASSERTION_VERSION = 1
    private const val AES_BLOCK_SIZE = 16
    private const val SECURE_MESSAGE_AES_KEY_LENGTH = 32
    private const val SECURE_MESSAGE_HEADER_DIGEST_LENGTH = 20
    private const val SECURE_MESSAGE_CIPHER = "AES/CBC/PKCS5Padding"
    private val SECURE_MESSAGE_ENCRYPTION_INFO = "ENC:2".toByteArray(StandardCharsets.UTF_8)
}
