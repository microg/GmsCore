/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac.storage

import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.profile.Build
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "RecaptchaQr"
private const val GCM_TAG_BITS = 128
private const val GCM_IV_BYTES = 12
private const val BASE64_FLAGS = Base64.URL_SAFE
private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"

internal object ModacSignerCrypto {

    fun fetchAndDecrypt(key: String, cache: ModacSignerCache): ByteArray? {
        if (Build.VERSION.SDK_INT < 23) {
            Log.d(TAG, "Encrypted credential cache requires Android 6.0 or newer")
            return null
        }
        val payload = cache.read(key) ?: return null
        val cipherBytes = try {
            Base64.decode(payload, BASE64_FLAGS)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Credential cache Base64 decode failed", e)
            cache.delete(key)
            return null
        }
        if (cipherBytes.size <= GCM_IV_BYTES) {
            Log.w(TAG, "Credential cache payload is too short (${cipherBytes.size}B)")
            cache.delete(key)
            return null
        }

        val secretKey = ModacKeystore.getOrCreateKey() ?: run {
            Log.w(TAG, "Credential cache AES key unavailable")
            return null
        }
        return Api23Impl.decrypt(secretKey, cipherBytes).also { decrypted ->
            if (decrypted == null) cache.delete(key)
        }
    }

    fun encryptAndStore(key: String, cache: ModacSignerCache, plainBytes: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            Log.d(TAG, "Encrypted credential cache requires Android 6.0 or newer")
            return false
        }
        val secretKey = ModacKeystore.getOrCreateKey() ?: run {
            Log.w(TAG, "Credential cache AES key unavailable")
            return false
        }
        val encoded = Api23Impl.encrypt(secretKey, plainBytes) ?: return false
        return cache.write(key, encoded)
    }

    @RequiresApi(23)
    private object Api23Impl {
        fun decrypt(secretKey: SecretKey, cipherBytes: ByteArray): ByteArray? = try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(GCM_TAG_BITS, cipherBytes, 0, GCM_IV_BYTES),
            )
            cipher.doFinal(cipherBytes, GCM_IV_BYTES, cipherBytes.size - GCM_IV_BYTES)
        } catch (e: Exception) {
            Log.w(TAG, "Credential cache AES/GCM decrypt failed", e)
            null
        }

        fun encrypt(secretKey: SecretKey, plainBytes: ByteArray): String? = try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val cipherBytes = cipher.doFinal(plainBytes)
            Base64.encodeToString(cipher.iv + cipherBytes, BASE64_FLAGS or Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "Credential cache AES/GCM encrypt failed", e)
            null
        }
    }
}
