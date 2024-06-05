/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.screenlock

import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build.VERSION.SDK_INT
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.utils.toBase64
import java.security.*
import java.security.cert.Certificate
import java.security.spec.ECGenParameterSpec
import kotlin.random.Random

@RequiresApi(23)
class ScreenLockCredentialStore(val context: Context) {
    private val keyStore by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }

    private fun getAlias(rpId: String, keyId: ByteArray): String =
        "1." + keyId.toBase64(Base64.NO_PADDING, Base64.NO_WRAP) + "." + rpId

    private fun getPrivateKey(rpId: String, keyId: ByteArray) = keyStore.getKey(getAlias(rpId, keyId), null) as? PrivateKey

    @RequiresApi(23)
    fun createKey(rpId: String, challenge: ByteArray): ByteArray {
        var useStrongbox = false
        if (SDK_INT >= 28) useStrongbox = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        val keyId = Random.nextBytes(32)
        val identifier = getAlias(rpId, keyId)
        Log.d(TAG, "Creating key for $identifier")
        val generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(identifier, KeyProperties.PURPOSE_SIGN)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setUserAuthenticationRequired(true)
        if (SDK_INT >= 28) builder.setIsStrongBoxBacked(useStrongbox)
        if (SDK_INT >= 24) builder.setAttestationChallenge(challenge)

        var generatedKeypair = false
        val exceptionClassesCaught = HashSet<Class<Exception>>()
        while (!generatedKeypair) {
            try {
                generator.initialize(builder.build())
                generator.generateKeyPair()
                generatedKeypair = true
            } catch (e: Exception) {
                // Catch each exception class at most once.
                // If we've caught the exception before, tried to correct it, and still catch the
                // same exception, then we can't fix it and the exception should be thrown further
                if (exceptionClassesCaught.contains(e.javaClass)) {
                    throw e
                }
                exceptionClassesCaught.add(e.javaClass)

                if (SDK_INT >= 28 && e is StrongBoxUnavailableException) {
                    Log.w(TAG, "Failed with StrongBox, retrying without it...")
                    // Not all algorithms are backed by the Strongbox. If the Strongbox doesn't
                    // support this keypair, fall back to TEE
                    builder.setIsStrongBoxBacked(false)
                } else if (SDK_INT >= 24 && e is ProviderException) {
                    Log.w(TAG, "Failed with attestation challenge, retrying without it...")
                    // This ProviderException is often thrown if the TEE or Strongbox doesn't have
                    // a built-in key to attest the new key pair with. If this happens, remove the
                    // attestation challenge and create an unattested key
                    builder.setAttestationChallenge(null)
                } else {
                    // We don't know how to handle other errors, so they should be thrown up the
                    // system
                    throw e
                }
            }
        }

        return keyId
    }

    fun getPublicKey(rpId: String, keyId: ByteArray): PublicKey? =
        keyStore.getCertificate(getAlias(rpId, keyId))?.publicKey

    fun getCertificateChain(rpId: String, keyId: ByteArray): Array<Certificate> =
        keyStore.getCertificateChain(getAlias(rpId, keyId))

    fun getSignature(rpId: String, keyId: ByteArray): Signature? {
        try {
            val privateKey = getPrivateKey(rpId, keyId) ?: return null
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            return signature
        } catch (e: KeyPermanentlyInvalidatedException) {
            keyStore.deleteEntry(getAlias(rpId, keyId))
            throw e
        }
    }

    fun containsKey(rpId: String, keyId: ByteArray): Boolean = keyStore.containsAlias(getAlias(rpId, keyId))

    companion object {
        const val TAG = "FidoLockStore"
    }
}
