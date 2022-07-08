/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.utils.toBase64
import java.security.*
import java.security.spec.ECGenParameterSpec
import kotlin.random.Random

const val TAG = "FidoApi"

@RequiresApi(23)
class InternalCredentialStore(val context: Context) {
    private val keyStore by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }

    private fun getAlias(rpId: String, keyId: ByteArray): String =
        "1." + keyId.toBase64(Base64.NO_PADDING, Base64.NO_WRAP) + "." + rpId

    private fun getPrivateKey(rpId: String, keyId: ByteArray) = keyStore.getKey(getAlias(rpId, keyId), null) as? PrivateKey

    fun createKey(rpId: String): ByteArray {
        val keyId = Random.nextBytes(32)
        val identifier = getAlias(rpId, keyId)
        Log.d(TAG, "Creating key for $identifier")
        val generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
        generator.initialize(
            KeyGenParameterSpec.Builder(identifier, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setUserAuthenticationRequired(true)
                .build()
        )
        generator.generateKeyPair()
        return keyId
    }

    fun getPublicKey(rpId: String, keyId: ByteArray): PublicKey? =
        keyStore.getCertificate(getAlias(rpId, keyId))?.publicKey

    fun getSignature(rpId: String, keyId: ByteArray): Signature? {
        val privateKey = getPrivateKey(rpId, keyId) ?: return null
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        return signature
    }

    fun containsKey(rpId: String, keyId: ByteArray): Boolean = keyStore.containsAlias(getAlias(rpId, keyId))
}
