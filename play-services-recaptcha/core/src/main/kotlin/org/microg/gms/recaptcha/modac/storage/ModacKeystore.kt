/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.profile.Build
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private const val TAG = "RecaptchaQr"
private const val KEY_ALIAS = "recck"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"

internal object ModacKeystore {

    @Synchronized
    fun getOrCreateKey(): SecretKey? {
        if (Build.VERSION.SDK_INT < 23) {
            Log.w(TAG, "AES/GCM Keystore requires Android 6.0 or newer")
            return null
        }
        return Api23Impl.getOrCreateKey()
    }

    @RequiresApi(23)
    private object Api23Impl {
        fun getOrCreateKey(): SecretKey? = try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)
                ?.secretKey
                ?: generateKey()
        } catch (e: Exception) {
            Log.w(TAG, "AndroidKeyStore unavailable", e)
            null
        }

        private fun generateKey(): SecretKey? = try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.w(TAG, "AndroidKeyStore key generation failed", e)
            null
        }
    }
}
