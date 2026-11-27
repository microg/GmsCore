/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import com.google.security.cryptauth.lib.securegcm.DeviceToDeviceMessage
import com.google.security.cryptauth.lib.securegcm.GcmMetadata
import com.google.security.cryptauth.lib.securegcm.Type
import com.google.security.cryptauth.lib.securemessage.EcP256PublicKey
import com.google.security.cryptauth.lib.securemessage.EncScheme
import com.google.security.cryptauth.lib.securemessage.GenericPublicKey
import com.google.security.cryptauth.lib.securemessage.Header
import com.google.security.cryptauth.lib.securemessage.HeaderAndBody
import com.google.security.cryptauth.lib.securemessage.PublicKeyType
import com.google.security.cryptauth.lib.securemessage.SecureMessage
import com.google.security.cryptauth.lib.securemessage.SigScheme
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class NearbyCrypto(val isServer: Boolean) {
    val keyPair: KeyPair = generateECKeyPair()
    val random: SecureRandom = SecureRandom()

    var decryptKey: ByteArray? = null
    var encryptKey: ByteArray? = null
    var recvHmacKey: ByteArray? = null
    var sendHmacKey: ByteArray? = null

    var authKey: ByteArray? = null
    var pinCode: String = ""

    private var sendSeq: Int = 0
    private var recvSeq: Int = 0

    fun getGenericPublicKey(): GenericPublicKey {
        val ecPub = keyPair.public as ECPublicKey
        val x = bigIntegerTo32Bytes(ecPub.w.affineX)
        val y = bigIntegerTo32Bytes(ecPub.w.affineY)
        return GenericPublicKey(
            type = PublicKeyType.EC_P256,
            ec_p256_public_key = EcP256PublicKey(x = x, y = y)
        )
    }

    fun completeKeyExchange(
        peerGenericKey: GenericPublicKey,
        clientInitBytes: ByteArray,
        serverInitBytes: ByteArray
    ) {
        val ecP256 = peerGenericKey.ec_p256_public_key
            ?: throw IllegalArgumentException("Missing ec_p256_public_key")
        val peerPublicKey = decodeECPublicKey(ecP256.x.toByteArray(), ecP256.y.toByteArray())

        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(keyPair.private)
        ka.doPhase(peerPublicKey, true)
        val sharedSecret = ka.generateSecret()

        val sha = MessageDigest.getInstance("SHA-256")
        val derivedSecret = sha.digest(sharedSecret)

        val ukeyInfo = ByteArray(clientInitBytes.size + serverInitBytes.size)
        System.arraycopy(clientInitBytes, 0, ukeyInfo, 0, clientInitBytes.size)
        System.arraycopy(serverInitBytes, 0, ukeyInfo, clientInitBytes.size, serverInitBytes.size)

        val authString = hkdf(
            ikm = derivedSecret,
            salt = "UKEY2 v1 auth".toByteArray(StandardCharsets.UTF_8),
            info = ukeyInfo,
            length = 32
        )
        val nextSecret = hkdf(
            ikm = derivedSecret,
            salt = "UKEY2 v1 next".toByteArray(StandardCharsets.UTF_8),
            info = ukeyInfo,
            length = 32
        )

        authKey = authString
        pinCode = pinCodeFromAuthKey(authString)

        val d2dClientKey = hkdf(
            ikm = nextSecret,
            salt = NearbySharingConstants.D2D_SALT,
            info = "client".toByteArray(StandardCharsets.UTF_8),
            length = 32
        )
        val d2dServerKey = hkdf(
            ikm = nextSecret,
            salt = NearbySharingConstants.D2D_SALT,
            info = "server".toByteArray(StandardCharsets.UTF_8),
            length = 32
        )

        val smsgSalt = sha.digest("SecureMessage".toByteArray(StandardCharsets.UTF_8))

        val clientKey = hkdf(d2dClientKey, smsgSalt, "ENC:2".toByteArray(StandardCharsets.UTF_8), 32)
        val clientHmacKey = hkdf(d2dClientKey, smsgSalt, "SIG:1".toByteArray(StandardCharsets.UTF_8), 32)
        val serverKey = hkdf(d2dServerKey, smsgSalt, "ENC:2".toByteArray(StandardCharsets.UTF_8), 32)
        val serverHmacKey = hkdf(d2dServerKey, smsgSalt, "SIG:1".toByteArray(StandardCharsets.UTF_8), 32)

        if (isServer) {
            decryptKey = clientKey
            recvHmacKey = clientHmacKey
            encryptKey = serverKey
            sendHmacKey = serverHmacKey
        } else {
            decryptKey = serverKey
            recvHmacKey = serverHmacKey
            encryptKey = clientKey
            sendHmacKey = clientHmacKey
        }
    }

    fun encryptOfflineFrame(frameBytes: ByteArray): ByteArray {
        val encKey = encryptKey ?: throw IllegalStateException("Keys not initialized")
        val hmacKey = sendHmacKey ?: throw IllegalStateException("Keys not initialized")

        sendSeq++
        val d2d = DeviceToDeviceMessage(
            sequence_number = sendSeq,
            message = frameBytes.toByteString()
        )
        val d2dBytes = d2d.encode()

        val iv = ByteArray(16).apply { random.nextBytes(this) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(iv))
        val encryptedBody = cipher.doFinal(d2dBytes)

        val metadata = GcmMetadata(
            type = Type.DEVICE_TO_DEVICE_MESSAGE,
            version = 1
        )

        val header = Header(
            encryption_scheme = EncScheme.AES_256_CBC,
            signature_scheme = SigScheme.HMAC_SHA256,
            iv = iv.toByteString(),
            public_metadata = metadata.encode().toByteString()
        )

        val headerAndBody = HeaderAndBody(
            header_ = header,
            body = encryptedBody.toByteString()
        )
        val hbBytes = headerAndBody.encode()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
        val signature = mac.doFinal(hbBytes)

        val smsg = SecureMessage(
            header_and_body = hbBytes.toByteString(),
            signature = signature.toByteString()
        )
        return smsg.encode()
    }

    fun decryptSecureMessage(secureMessageBytes: ByteArray): ByteArray {
        val decKey = decryptKey ?: throw IllegalStateException("Keys not initialized")
        val hmacKey = recvHmacKey ?: throw IllegalStateException("Keys not initialized")

        val smsg = SecureMessage.ADAPTER.decode(secureMessageBytes)
        val hbBytes = smsg.header_and_body.toByteArray()
        val signature = smsg.signature.toByteArray()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
        val expectedSig = mac.doFinal(hbBytes)
        if (!MessageDigest.isEqual(expectedSig, signature)) {
            throw SecurityException("HMAC signature mismatch")
        }

        val headerAndBody = HeaderAndBody.ADAPTER.decode(hbBytes)
        val iv = headerAndBody.header_.iv?.toByteArray()
            ?: throw IllegalArgumentException("Missing IV in Header")
        val cipherText = headerAndBody.body.toByteArray()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decKey, "AES"), IvParameterSpec(iv))
        val decryptedD2D = cipher.doFinal(cipherText)

        val d2d = DeviceToDeviceMessage.ADAPTER.decode(decryptedD2D)
        recvSeq++
        if (d2d.sequence_number != recvSeq) {
            throw SecurityException("Invalid sequence number: expected $recvSeq got ${d2d.sequence_number}")
        }

        return d2d.message?.toByteArray() ?: ByteArray(0)
    }

    companion object {
        fun generateECKeyPair(): KeyPair {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            return kpg.generateKeyPair()
        }

        fun bigIntegerTo32Bytes(b: BigInteger): ByteString {
            val bytes = b.toByteArray()
            val result = ByteArray(32)
            if (bytes.size >= 32) {
                System.arraycopy(bytes, bytes.size - 32, result, 0, 32)
            } else {
                System.arraycopy(bytes, 0, result, 32 - bytes.size, bytes.size)
            }
            return result.toByteString()
        }

        fun decodeECPublicKey(xBytes: ByteArray, yBytes: ByteArray): PublicKey {
            val x = BigInteger(1, xBytes)
            val y = BigInteger(1, yBytes)
            val point = ECPoint(x, y)

            val params = AlgorithmParameters.getInstance("EC")
            params.init(ECGenParameterSpec("secp256r1"))
            val ecSpec = params.getParameterSpec(ECParameterSpec::class.java)

            val kf = KeyFactory.getInstance("EC")
            return kf.generatePublic(ECPublicKeySpec(point, ecSpec))
        }

        fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
            val prk = hkdfExtract(salt, ikm)
            return hkdfExpand(prk, info, length)
        }

        fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            val key = if (salt.isEmpty()) ByteArray(32) else salt
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(ikm)
        }

        fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            val okm = ByteArray(length)
            var t = ByteArray(0)
            var offset = 0
            var i = 1
            while (offset < length) {
                mac.reset()
                mac.update(t)
                mac.update(info)
                mac.update(i.toByte())
                t = mac.doFinal()
                val chunkLen = Math.min(t.size, length - offset)
                System.arraycopy(t, 0, okm, offset, chunkLen)
                offset += chunkLen
                i++
            }
            return okm
        }

        fun pinCodeFromAuthKey(authKey: ByteArray): String {
            var hash = 0
            var multiplier = 1
            for (b in authKey) {
                val byteVal = b.toInt()
                hash = (hash + byteVal * multiplier) % 9973
                multiplier = (multiplier * 31) % 9973
            }
            return String.format(Locale.US, "%04d", Math.abs(hash))
        }
    }
}
