/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.recaptcha.qr.CachedInitCredential

private const val TAG = "RecaptchaQr"
private const val CACHE_KEY_SUFFIX = "_init"

internal class RecaptchaCredentialStore(context: Context) {
    private val cache = ModacSignerCache(context.applicationContext)

    suspend fun read(siteKey: String): CachedInitCredential? = withContext(Dispatchers.IO) {
        readCredential(cacheKey(siteKey))
    }

    suspend fun write(siteKey: String, credential: CachedInitCredential): Boolean = withContext(Dispatchers.IO) {
        ModacSignerCrypto.encryptAndStore(cacheKey(siteKey), cache, credential.encode())
    }

    suspend fun replaceIfCurrent(
        siteKey: String,
        expectedCredential: String,
        replacement: CachedInitCredential,
    ): Boolean = withContext(Dispatchers.IO) {
        val key = cacheKey(siteKey)
        cache.withExclusiveAccess {
            val current = readCredential(key, " during update") ?: return@withExclusiveAccess false
            if (current.credential != expectedCredential) return@withExclusiveAccess false
            ModacSignerCrypto.encryptAndStore(key, cache, replacement.encode())
        }
    }

    private fun readCredential(key: String, logContext: String = ""): CachedInitCredential? {
        val decrypted = ModacSignerCrypto.fetchAndDecrypt(key, cache) ?: return null
        return try {
            CachedInitCredential.ADAPTER.decode(decrypted)
        } catch (e: Exception) {
            Log.w(TAG, "Cached credential decode failed$logContext", e)
            cache.delete(key)
            null
        }
    }

    private fun cacheKey(siteKey: String): String = siteKey + CACHE_KEY_SUFFIX
}
