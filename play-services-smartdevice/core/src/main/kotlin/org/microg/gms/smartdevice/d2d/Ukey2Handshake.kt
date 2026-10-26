/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import okio.ByteString.Companion.toByteString
import com.google.security.cryptauth.lib.securegcm.DeviceToDeviceMessage
import com.google.security.cryptauth.lib.securegcm.GcmMetadata
import com.google.security.cryptauth.lib.securegcm.Type as GcmMessageType
import com.google.security.cryptauth.lib.securegcm.Ukey2ClientFinished
import com.google.security.cryptauth.lib.securegcm.Ukey2ClientInit
import com.google.security.cryptauth.lib.securegcm.Ukey2HandshakeCipher
import com.google.security.cryptauth.lib.securegcm.Ukey2Message
import com.google.security.cryptauth.lib.securegcm.Ukey2ServerInit
import com.google.security.cryptauth.lib.securemessage.EncScheme
import com.google.security.cryptauth.lib.securemessage.GenericPublicKey
import com.google.security.cryptauth.lib.securemessage.Header
import com.google.security.cryptauth.lib.securemessage.HeaderAndBodyInternal
import com.google.security.cryptauth.lib.securemessage.PublicKeyType
import com.google.security.cryptauth.lib.securemessage.SecureMessage
import com.google.security.cryptauth.lib.securemessage.SigScheme
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.SignatureException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.microg.gms.utils.hkdfSha256

internal class Ukey2Handshake(
    role: Int = UKEY2_VERSION_V2,
) {
    enum class Stage {
        IDLE,
        WAITING_FOR_CLIENT_INIT,
        WAITING_FOR_CLIENT_FINISHED,
        COMPLETED,
        FAILED,
    }

    @Volatile
    var stage: Stage = Stage.IDLE
        private set

    @Volatile
    var cipher: Ukey2Cipher? = null
        private set

    private val outbox = ArrayDeque<ByteArray>()
    private val secureRandom = SecureRandom()
    private val ourKeyPair: KeyPair = generateP256KeyPair()
    private var clientCommitment: ByteArray? = null
    private var rawClientInit: ByteArray? = null
    private var rawServerInit: ByteArray? = null

    init {
        require(role == UKEY2_VERSION_V2) { "DirectTransfer source must use the UKEY2 responder role" }
    }

    @Throws(SignatureException::class)
    fun onFirstByte(selection: Int) {
        check(stage == Stage.IDLE) { "UKEY2 handshake already started" }
        if (selection != ENCRYPTION_ALGORITHM_UKEY2) {
            stage = Stage.FAILED
            throw SignatureException("Unknown encryption selection: 0x" + selection.toString(16))
        }
        stage = Stage.WAITING_FOR_CLIENT_INIT
    }

    @Throws(SignatureException::class)
    fun consume(peerBytes: ByteArray) {
        try {
            when (stage) {
                Stage.WAITING_FOR_CLIENT_INIT -> consumeClientInit(peerBytes)
                Stage.WAITING_FOR_CLIENT_FINISHED -> consumeClientFinished(peerBytes)
                Stage.COMPLETED -> throw SignatureException("UKEY2 handshake is already complete")
                Stage.IDLE -> throw SignatureException("UKEY2 handshake was not started")
                Stage.FAILED -> throw SignatureException("UKEY2 handshake has failed")
            }
        } catch (e: SignatureException) {
            stage = Stage.FAILED
            throw e
        } catch (e: Exception) {
            stage = Stage.FAILED
            throw SignatureException("Invalid UKEY2 handshake message", e)
        }
    }

    fun isCompleted(): Boolean = stage == Stage.COMPLETED && cipher != null

    fun pollOutbound(): ByteArray? = outbox.removeFirstOrNull()

    private fun consumeClientInit(raw: ByteArray) {
        val envelope = Ukey2Wire.parseEnvelope(raw)
        require(envelope.type == Ukey2Wire.TYPE_CLIENT_INIT) { "Expected UKEY2 ClientInit" }

        val init = Ukey2Wire.parseClientInit(envelope.data)
        require(init.version == Ukey2Wire.VERSION) { "Unsupported UKEY2 version " + init.version }
        require(init.random.size == Ukey2Wire.NONCE_LENGTH) { "Invalid UKEY2 client random" }
        require(init.nextProtocol == Ukey2Wire.NEXT_PROTOCOL) { "Unsupported UKEY2 next protocol" }
        clientCommitment = init.commitments.firstOrNull { it.cipher == Ukey2Wire.CIPHER_P256_SHA512 }
            ?.commitment
            ?: throw SignatureException("Client did not offer P256_SHA512")
        rawClientInit = raw.copyOf()

        val serverInit = Ukey2Wire.encodeServerInit(
            random = ByteArray(Ukey2Wire.NONCE_LENGTH).also(secureRandom::nextBytes),
            publicKey = AuthZenWire.encodeGenericPublicKey(ourKeyPair.public as ECPublicKey),
        )
        rawServerInit = serverInit
        outbox.addLast(serverInit)
        stage = Stage.WAITING_FOR_CLIENT_FINISHED
    }

    private fun consumeClientFinished(raw: ByteArray) {
        val expected = clientCommitment ?: throw SignatureException("Missing client commitment")
        val actual = MessageDigest.getInstance("SHA-512").digest(raw)
        if (!MessageDigest.isEqual(expected, actual)) {
            throw SignatureException("UKEY2 ClientFinished commitment does not match")
        }

        val envelope = Ukey2Wire.parseEnvelope(raw)
        require(envelope.type == Ukey2Wire.TYPE_CLIENT_FINISH) { "Expected UKEY2 ClientFinished" }
        val peerKey = Ukey2Wire.parseClientFinished(envelope.data)

        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(ourKeyPair.private)
        agreement.doPhase(peerKey, true)

        val sharedSecret = MessageDigest.getInstance("SHA-256").digest(agreement.generateSecret())
        val transcript = Ukey2Wire.concat(
            rawClientInit ?: throw SignatureException("Missing ClientInit transcript"),
            rawServerInit ?: throw SignatureException("Missing ServerInit transcript"),
        )
        val nextKey = hkdfSha256(
            sharedSecret,
            Ukey2Wire.NEXT_SALT,
            transcript,
            Ukey2Wire.KEY_LENGTH,
        )
        val clientKey = hkdfSha256(
            nextKey,
            Ukey2Crypto.D2D_SALT,
            Ukey2Wire.CLIENT_PURPOSE,
            Ukey2Wire.KEY_LENGTH,
        )
        val serverKey = hkdfSha256(
            nextKey,
            Ukey2Crypto.D2D_SALT,
            Ukey2Wire.SERVER_PURPOSE,
            Ukey2Wire.KEY_LENGTH,
        )

        cipher = Ukey2Cipher(encodeKey = serverKey, decodeKey = clientKey, secureRandom = secureRandom)
        outbox.addLast(Ukey2Wire.FINAL_ACK.copyOf())
        stage = Stage.COMPLETED
    }

    private fun generateP256KeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        try {
            generator.initialize(ECGenParameterSpec("prime256v1"), secureRandom)
        } catch (_: Exception) {
            generator.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        }
        return generator.generateKeyPair()
    }
}

internal class Ukey2Cipher(
    private val encodeKey: ByteArray,
    private val decodeKey: ByteArray,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    private var encodeSequence = 0
    private var decodeSequence = 0

    @Synchronized
    @Throws(SignatureException::class)
    fun encrypt(plaintext: ByteArray): ByteArray {
        return try {
            encodeSequence = Math.incrementExact(encodeSequence)
            val d2dMessage = DeviceToDeviceMessage(
                message = plaintext.toByteString(),
                sequenceNumber = encodeSequence,
            )
            val d2dPayload = DeviceToDeviceMessage.ADAPTER.encode(d2dMessage)
            Ukey2Crypto.signcrypt(d2dPayload, encodeKey, secureRandom)
        } catch (e: Exception) {
            throw SignatureException("Unable to encrypt UKEY2 D2D message", e)
        }
    }

    @Synchronized
    @Throws(SignatureException::class)
    fun decrypt(ciphertext: ByteArray): ByteArray {
        try {
            val d2dPayload = Ukey2Crypto.verifyDecrypt(ciphertext, decodeKey)
            val d2dMessage = DeviceToDeviceMessage.ADAPTER.decode(d2dPayload)
            val sequenceNumber = d2dMessage.sequenceNumber
            val expectedSequence = Math.incrementExact(decodeSequence)
            if (sequenceNumber != expectedSequence) {
                throw SignatureException(
                    "Incorrect UKEY2 sequence number: expected " + expectedSequence + ", got " + sequenceNumber,
                )
            }
            decodeSequence = expectedSequence
            return d2dMessage.message?.toByteArray()
                ?: throw SignatureException("UKEY2 D2D message has no payload")
        } catch (e: SignatureException) {
            throw e
        } catch (e: Exception) {
            throw SignatureException("Unable to decrypt UKEY2 D2D message", e)
        }
    }
}

private object Ukey2Crypto {
    val D2D_SALT: ByteArray = MessageDigest.getInstance("SHA-256")
        .digest("D2D".toByteArray(StandardCharsets.UTF_8))
    private val secureMessageSalt: ByteArray = MessageDigest.getInstance("SHA-256")
        .digest("SecureMessage".toByteArray(StandardCharsets.UTF_8))

    fun signcrypt(payload: ByteArray, masterKey: ByteArray, random: SecureRandom): ByteArray {
        val iv = ByteArray(16).also(random::nextBytes)
        val metadata = GcmMetadata(
            type = GcmMessageType.DEVICE_TO_DEVICE_MESSAGE,
            version = Ukey2Wire.SECURE_GCM_VERSION,
        )
        val header = Header(
            signatureScheme = SigScheme.HMAC_SHA256,
            encryptionScheme = EncScheme.AES_256_CBC,
            iv = iv.toByteString(),
            publicMetadata = GcmMetadata.ADAPTER.encode(metadata).toByteString(),
        )
        val encodedHeader = Header.ADAPTER.encode(header)

        val encryptedBody = aesCbc(
            mode = Cipher.ENCRYPT_MODE,
            key = deriveSubKey(masterKey, "ENC:2"),
            iv = iv,
            input = payload,
        )
        val signedData = HeaderAndBodyInternal(
            header_ = encodedHeader.toByteString(),
            body = encryptedBody.toByteString(),
        )
        val headerAndBody = HeaderAndBodyInternal.ADAPTER.encode(signedData)
        val signature = hmac(deriveSubKey(masterKey, "SIG:1"), headerAndBody)
        val secureMessage = SecureMessage(
            headerAndBody = headerAndBody.toByteString(),
            signature = signature.toByteString(),
        )
        return SecureMessage.ADAPTER.encode(secureMessage)
    }

    @Throws(SignatureException::class)
    fun verifyDecrypt(message: ByteArray, masterKey: ByteArray): ByteArray {
        val secureMessage = SecureMessage.ADAPTER.decode(message)
        val signed = secureMessage.headerAndBody.toByteArray()
        val receivedSignature = secureMessage.signature.toByteArray()
        val expectedSignature = hmac(deriveSubKey(masterKey, "SIG:1"), signed)
        if (!MessageDigest.isEqual(expectedSignature, receivedSignature)) {
            throw SignatureException("UKEY2 message signature failed verification")
        }

        val signedData = HeaderAndBodyInternal.ADAPTER.decode(signed)
        val header = Header.ADAPTER.decode(signedData.header_.toByteArray())
        if (header.signatureScheme != SigScheme.HMAC_SHA256 || header.encryptionScheme != EncScheme.AES_256_CBC) {
            throw SignatureException("Unsupported SecureMessage algorithms")
        }
        val iv = header.iv?.toByteArray()
        if (iv?.size != 16) throw SignatureException("SecureMessage has invalid IV")
        val metadata = header.publicMetadata
            ?: throw SignatureException("SecureMessage missing metadata")
        validateMetadata(metadata.toByteArray())
        return aesCbc(
            mode = Cipher.DECRYPT_MODE,
            key = deriveSubKey(masterKey, "ENC:2"),
            iv = iv,
            input = signedData.body.toByteArray(),
        )
    }

    private fun validateMetadata(encoded: ByteArray) {
        val metadata = GcmMetadata.ADAPTER.decode(encoded)
        if (metadata.type != GcmMessageType.DEVICE_TO_DEVICE_MESSAGE) {
            throw SignatureException("Wrong UKEY2 D2D payload type")
        }
        val version = metadata.version ?: 0
        if (version > Ukey2Wire.SECURE_GCM_VERSION) {
            throw SignatureException("Unsupported SecureGCM version " + version)
        }
    }

    private fun deriveSubKey(masterKey: ByteArray, purpose: String): ByteArray =
        hkdfSha256(
            masterKey,
            secureMessageSalt,
            purpose.toByteArray(StandardCharsets.UTF_8),
            Ukey2Wire.KEY_LENGTH,
        )

    private fun aesCbc(mode: Int, key: ByteArray, iv: ByteArray, input: ByteArray): ByteArray =
        Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            doFinal(input)
        }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(data)
        }
}

private object Ukey2Wire {
    const val VERSION = 1
    const val NONCE_LENGTH = 32
    const val KEY_LENGTH = 32
    const val CIPHER_P256_SHA512 = 100
    const val TYPE_CLIENT_INIT = 2
    const val TYPE_CLIENT_FINISH = 4
    const val SECURE_GCM_VERSION = 1
    const val NEXT_PROTOCOL = "AES_256_CBC-HMAC_SHA256"

    val NEXT_SALT = "UKEY2 v1 next".toByteArray(StandardCharsets.UTF_8)
    val CLIENT_PURPOSE = "client".toByteArray(StandardCharsets.UTF_8)
    val SERVER_PURPOSE = "server".toByteArray(StandardCharsets.UTF_8)
    val FINAL_ACK = byteArrayOf(1, 1, 2, 3, 5, 8)

    data class Envelope(val type: Int, val data: ByteArray)
    data class Commitment(val cipher: Int, val commitment: ByteArray)
    data class ClientInit(
        val version: Int,
        val random: ByteArray,
        val commitments: List<Commitment>,
        val nextProtocol: String,
    )

    fun parseEnvelope(encoded: ByteArray): Envelope {
        val message = Ukey2Message.ADAPTER.decode(encoded)
        return Envelope(
            message.messageType?.value ?: throw SignatureException("UKEY2 message missing type"),
            message.messageData?.toByteArray() ?: throw SignatureException("UKEY2 message missing data"),
        )
    }

    fun parseClientInit(encoded: ByteArray): ClientInit {
        val message = Ukey2ClientInit.ADAPTER.decode(encoded)
        return ClientInit(
            message.version ?: throw SignatureException("ClientInit missing version"),
            message.random?.toByteArray() ?: throw SignatureException("ClientInit missing random"),
            message.cipherCommitments.map { parseCommitment(it) },
            message.nextProtocol ?: throw SignatureException("ClientInit missing next protocol"),
        )
    }

    private fun parseCommitment(message: Ukey2ClientInit.CipherCommitment): Commitment {
        return Commitment(
            message.handshakeCipher?.value ?: throw SignatureException("Cipher commitment missing cipher"),
            message.commitment?.toByteArray() ?: throw SignatureException("Cipher commitment missing hash"),
        )
    }

    fun encodeServerInit(random: ByteArray, publicKey: ByteArray): ByteArray {
        val serverInit = Ukey2ServerInit(
            version = VERSION,
            random = random.toByteString(),
            handshakeCipher = Ukey2HandshakeCipher.P256_SHA512,
            publicKey = publicKey.toByteString(),
        )
        val message = Ukey2Message(
            messageType = Ukey2Message.Type.SERVER_INIT,
            messageData = Ukey2ServerInit.ADAPTER.encode(serverInit).toByteString(),
        )
        return Ukey2Message.ADAPTER.encode(message)
    }

    fun parseClientFinished(encoded: ByteArray): ECPublicKey {
        val message = Ukey2ClientFinished.ADAPTER.decode(encoded)
        val publicKey = message.publicKey ?: throw SignatureException("ClientFinished missing public key")
        return parsePublicKey(publicKey.toByteArray())
    }

    private fun parsePublicKey(encoded: ByteArray): ECPublicKey {
        val publicKey = GenericPublicKey.ADAPTER.decode(encoded)
        if (publicKey.type != PublicKeyType.EC_P256) {
            throw SignatureException("UKEY2 public key is not EC_P256")
        }
        val ecKey = publicKey.ecP256PublicKey
            ?: throw SignatureException("UKEY2 public key has no EC point")
        val x = decodeCoordinate(ecKey.x.toByteArray())
        val y = decodeCoordinate(ecKey.y.toByteArray())

        val parameters = (KeyPairGenerator.getInstance("EC").run {
            try {
                initialize(ECGenParameterSpec("prime256v1"))
            } catch (_: Exception) {
                initialize(ECGenParameterSpec("secp256r1"))
            }
            generateKeyPair().public
        } as ECPublicKey).params
        return KeyFactory.getInstance("EC")
            .generatePublic(ECPublicKeySpec(ECPoint(x, y), parameters)) as ECPublicKey
    }

    private fun decodeCoordinate(encoded: ByteArray): BigInteger {
        if (encoded.isEmpty() || encoded.size > 33 || (encoded.size == 33 && encoded[0].toInt() != 0)) {
            throw SignatureException("Invalid P-256 coordinate encoding")
        }
        return BigInteger(encoded).also {
            if (it.signum() < 0) throw SignatureException("Negative P-256 coordinate")
        }
    }

    fun concat(first: ByteArray, second: ByteArray): ByteArray =
        ByteArray(first.size + second.size).also {
            System.arraycopy(first, 0, it, 0, first.size)
            System.arraycopy(second, 0, it, first.size, second.size)
        }
}
