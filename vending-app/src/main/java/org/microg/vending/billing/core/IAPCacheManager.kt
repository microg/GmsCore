package org.microg.vending.billing.core

import android.util.LruCache
import org.microg.gms.utils.digest
import org.microg.gms.utils.toHexString

operator fun <K, V> LruCache<K, V>.set(key: K, value: V) { put(key, value) }

class CacheEntry(
    val data: ByteArray,
    val expiredAt: Long
)

class IAPCacheManager(maxSize: Int = 1024, private val expireMs: Int = 7200000) {
    private val lruCache = LruCache<String, CacheEntry>(maxSize)

    @Synchronized
    fun get(requestBody: ByteArray): ByteArray? {
        val entry = lruCache[calculateHash(requestBody)]
        if (entry == null || entry.expiredAt < System.currentTimeMillis())
            return null
        return entry.data
    }

    @Synchronized
    fun put(requestBody: ByteArray, responseData: ByteArray) {
        val key = calculateHash(requestBody)
        lruCache[key] = CacheEntry(responseData, System.currentTimeMillis() + expireMs)
    }

    private fun calculateHash(body: ByteArray): String {
        return body.digest("SHA-256").toHexString()
    }
}
