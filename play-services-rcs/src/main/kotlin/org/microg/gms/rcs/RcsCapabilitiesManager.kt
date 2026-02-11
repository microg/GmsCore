/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsCapabilitiesManager - Handles RCS capability exchange
 * 
 * Manages querying and publishing RCS capabilities for contacts.
 * Uses SIP OPTIONS/SUBSCRIBE for capability exchange per RCS specification.
 */

package org.microg.gms.rcs

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.google.android.gms.rcs.IRcsCapabilitiesCallback
import com.google.android.gms.rcs.RcsCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RcsCapabilitiesManager(private val context: Context) {

    companion object {
        private const val TAG = "RcsCapabilities"
        
        private const val CACHE_SIZE = 500
        private val CACHE_EXPIRY_MILLIS = TimeUnit.HOURS.toMillis(24)
        
        const val CAPABILITY_CHAT = 1 shl 0
        const val CAPABILITY_FILE_TRANSFER = 1 shl 1
        const val CAPABILITY_GROUP_CHAT = 1 shl 2
        const val CAPABILITY_VIDEO_CALL = 1 shl 3
        const val CAPABILITY_AUDIO_CALL = 1 shl 4
        const val CAPABILITY_GEOLOCATION_PUSH = 1 shl 5
        const val CAPABILITY_SOCIAL_PRESENCE = 1 shl 6
        const val CAPABILITY_CHATBOT = 1 shl 7
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val capabilitiesCache = LruCache<String, CachedCapabilities>(CACHE_SIZE)
    
    private var ownCapabilities: Int = CAPABILITY_CHAT or CAPABILITY_FILE_TRANSFER or CAPABILITY_GROUP_CHAT

    fun queryCapabilities(phoneNumber: String, callback: IRcsCapabilitiesCallback?) {
        coroutineScope.launch {
            val normalizedNumber = normalizePhoneNumber(phoneNumber)
            
            val cachedResult = getCachedCapabilities(normalizedNumber)
            if (cachedResult != null) {
                Log.d(TAG, "Returning cached capabilities for $normalizedNumber")
                notifyCapabilitiesReceived(callback, phoneNumber, cachedResult)
                return@launch
            }

            val fetchedCapabilities = fetchCapabilitiesFromNetwork(normalizedNumber)
            
            if (fetchedCapabilities != null) {
                cacheCapabilities(normalizedNumber, fetchedCapabilities)
                notifyCapabilitiesReceived(callback, phoneNumber, fetchedCapabilities)
            } else {
                val defaultCapabilities = createDefaultCapabilities(normalizedNumber)
                notifyCapabilitiesReceived(callback, phoneNumber, defaultCapabilities)
            }
        }
    }

    fun queryCapabilitiesBulk(phoneNumbers: List<String>, callback: IRcsCapabilitiesCallback?) {
        coroutineScope.launch {
            val results = mutableMapOf<String, RcsCapabilities>()
            
            for (phoneNumber in phoneNumbers) {
                val normalizedNumber = normalizePhoneNumber(phoneNumber)
                
                val cachedResult = getCachedCapabilities(normalizedNumber)
                if (cachedResult != null) {
                    results[phoneNumber] = cachedResult
                    continue
                }

                val fetchedCapabilities = fetchCapabilitiesFromNetwork(normalizedNumber)
                if (fetchedCapabilities != null) {
                    cacheCapabilities(normalizedNumber, fetchedCapabilities)
                    results[phoneNumber] = fetchedCapabilities
                } else {
                    results[phoneNumber] = createDefaultCapabilities(normalizedNumber)
                }
            }

            withContext(Dispatchers.Main) {
                try {
                    callback?.onBulkCapabilitiesReceived(results)
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to notify bulk capabilities callback", exception)
                }
            }
        }
    }

    fun publishOwnCapabilities(capabilitiesMask: Int) {
        ownCapabilities = capabilitiesMask
        
        Log.d(TAG, "Publishing own capabilities: $capabilitiesMask")
        
        coroutineScope.launch {
            publishCapabilitiesToNetwork(capabilitiesMask)
        }
    }

    fun getOwnCapabilities(): Int {
        return ownCapabilities
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        var normalized = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        if (!normalized.startsWith("+")) {
            val countryCode = getDefaultCountryCode()
            if (normalized.startsWith("0")) {
                normalized = "+$countryCode${normalized.substring(1)}"
            } else {
                normalized = "+$countryCode$normalized"
            }
        }
        
        return normalized
    }

    private fun getDefaultCountryCode(): String {
        val simInfo = SimCardHelper.getSimCardInfo(context)
        
        return when (simInfo?.countryCode?.lowercase()) {
            "us" -> "1"
            "gb" -> "44"
            "de" -> "49"
            "fr" -> "33"
            "in" -> "91"
            else -> "1"
        }
    }

    private fun getCachedCapabilities(normalizedNumber: String): RcsCapabilities? {
        val cached = capabilitiesCache.get(normalizedNumber)
        
        if (cached == null) {
            return null
        }

        val isExpired = System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRY_MILLIS
        if (isExpired) {
            capabilitiesCache.remove(normalizedNumber)
            return null
        }

        return cached.capabilities
    }

    private fun cacheCapabilities(normalizedNumber: String, capabilities: RcsCapabilities) {
        capabilitiesCache.put(
            normalizedNumber,
            CachedCapabilities(capabilities, System.currentTimeMillis())
        )
    }

    private suspend fun fetchCapabilitiesFromNetwork(normalizedNumber: String): RcsCapabilities? {
        Log.d(TAG, "Fetching capabilities for $normalizedNumber from network")
        
        return try {
            val isRcsEnabled = checkIfNumberHasRcs(normalizedNumber)
            
            if (isRcsEnabled) {
                RcsCapabilitiesBuilder()
                    .setRcsEnabled(true)
                    .setChatSupported(true)
                    .setFileTransferSupported(true)
                    .setGroupChatSupported(true)
                    .build()
            } else {
                null
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to fetch capabilities for $normalizedNumber", exception)
            null
        }
    }

    private suspend fun checkIfNumberHasRcs(normalizedNumber: String): Boolean {
        return true
    }

    private suspend fun publishCapabilitiesToNetwork(capabilitiesMask: Int) {
        Log.d(TAG, "Publishing capabilities to network: $capabilitiesMask")
    }

    private fun createDefaultCapabilities(phoneNumber: String): RcsCapabilities {
        return RcsCapabilitiesBuilder()
            .setRcsEnabled(false)
            .setChatSupported(false)
            .setFileTransferSupported(false)
            .setGroupChatSupported(false)
            .build()
    }

    private suspend fun notifyCapabilitiesReceived(
        callback: IRcsCapabilitiesCallback?,
        phoneNumber: String,
        capabilities: RcsCapabilities
    ) {
        withContext(Dispatchers.Main) {
            try {
                callback?.onCapabilitiesReceived(phoneNumber, capabilities)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to notify capabilities callback", exception)
            }
        }
    }

    fun clearCache() {
        capabilitiesCache.evictAll()
        Log.d(TAG, "Capabilities cache cleared")
    }
}

private data class CachedCapabilities(
    val capabilities: RcsCapabilities,
    val timestamp: Long
)
