/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cryptauth

import android.content.Context
import android.util.Base64
import okio.ByteString.Companion.toByteString
import securemessage.EcP256PublicKey
import securemessage.GenericPublicKey
import securemessage.PublicKeyType
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

private const val PREFERENCES_NAME = "cryptauth_keys"
private const val PREF_PRIVATE_KEY = "private_key"
private const val PREF_PUBLIC_KEY = "public_key"

/**
 * Salt prepended to the random session id when computing the proof of an enrolled key.
 * See Chromium's `CryptAuthKeyProofComputerImpl`.
 */
private const val KEY_PROOF_SALT = "CryptAuth Key Proof"

/**
 * P256 user key pair enrolled in the `PublicKey` key bundle. Google expects this key to exist
 * after answering a SyncKeys request with `keyCreation: ACTIVE`.
 */
internal class CryptAuthUserKey(private val keyPair: KeyPair) {

    /** Serialized SecureMessage `GenericPublicKey`, the format CryptAuth expects for P256 keys. */
    val keyMaterial: ByteArray by lazy {
        val point = (keyPair.public as ECPublicKey).w
        GenericPublicKey(
            type = PublicKeyType.EC_P256,
            ec_p256_public_key = EcP256PublicKey(
                x = point.affineX.toTwosComplement().toByteString(),
                y = point.affineY.toTwosComplement().toByteString()
            )
        ).encode()
    }

    val handle: ByteArray by lazy { MessageDigest.getInstance("SHA-256").digest(keyMaterial) }

    /** ECDSA (SHA-256, DER encoded) signature over salt || random session id. */
    fun computeKeyProof(randomSessionId: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
        initSign(keyPair.private)
        update(KEY_PROOF_SALT.toByteArray())
        update(randomSessionId)
        sign()
    }
}

/** P256 coordinates are encoded as fixed 33 byte two's complement, as done by SecureMessage. */
private fun BigInteger.toTwosComplement(): ByteArray {
    val bytes = toByteArray()
    return if (bytes.size >= 33) bytes.copyOfRange(bytes.size - 33, bytes.size) else ByteArray(33 - bytes.size) + bytes
}

private fun keyPreferenceKey(accountName: String, key: String) = "$accountName:$key"

internal fun Context.getCryptAuthUserKey(accountName: String): CryptAuthUserKey? {
    val preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val privateKey = preferences.getString(keyPreferenceKey(accountName, PREF_PRIVATE_KEY), null) ?: return null
    val publicKey = preferences.getString(keyPreferenceKey(accountName, PREF_PUBLIC_KEY), null) ?: return null
    return try {
        val keyFactory = KeyFactory.getInstance("EC")
        CryptAuthUserKey(
            KeyPair(
                keyFactory.generatePublic(X509EncodedKeySpec(Base64.decode(publicKey, Base64.NO_WRAP))),
                keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.NO_WRAP)))
            )
        )
    } catch (e: Exception) {
        null
    }
}

internal fun Context.getOrCreateCryptAuthUserKey(accountName: String): CryptAuthUserKey {
    getCryptAuthUserKey(accountName)?.let { return it }
    val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
        .putString(keyPreferenceKey(accountName, PREF_PRIVATE_KEY), Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP))
        .putString(keyPreferenceKey(accountName, PREF_PUBLIC_KEY), Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP))
        .apply()
    return CryptAuthUserKey(keyPair)
}
