/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsEncryptedDataStore - Encrypted key-value storage
 */

package org.microg.gms.rcs.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class RcsEncryptedDataStore private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "rcs_encrypted_store"
        
        @Volatile
        private var instance: RcsEncryptedDataStore? = null
        
        fun getInstance(context: Context): RcsEncryptedDataStore {
            return instance ?: synchronized(this) {
                instance ?: RcsEncryptedDataStore(context.applicationContext).also { instance = it }
            }
        }
    }

    private val cache = ConcurrentHashMap<String, Any>()
    private val preferences: android.content.SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        preferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
        cache[key] = value
    }

    fun getString(key: String, default: String? = null): String? {
        return cache.getOrPut(key) {
            preferences.getString(key, default) ?: return default
        } as? String
    }

    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
        cache[key] = value
    }

    fun getInt(key: String, default: Int = 0): Int {
        return (cache.getOrPut(key) {
            preferences.getInt(key, default)
        } as? Int) ?: default
    }

    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
        cache[key] = value
    }

    fun getLong(key: String, default: Long = 0L): Long {
        return (cache.getOrPut(key) {
            preferences.getLong(key, default)
        } as? Long) ?: default
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
        cache[key] = value
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return (cache.getOrPut(key) {
            preferences.getBoolean(key, default)
        } as? Boolean) ?: default
    }

    fun putStringSet(key: String, values: Set<String>) {
        preferences.edit().putStringSet(key, values).apply()
        cache[key] = values
    }

    @Suppress("UNCHECKED_CAST")
    fun getStringSet(key: String, default: Set<String>? = null): Set<String>? {
        return cache.getOrPut(key) {
            preferences.getStringSet(key, default) ?: return default
        } as? Set<String>
    }

    fun putObject(key: String, obj: Any) {
        val json = JSONObject()
        when (obj) {
            is Map<*, *> -> {
                obj.forEach { (k, v) -> json.put(k.toString(), v) }
            }
            else -> {
                json.put("value", obj.toString())
            }
        }
        putString(key, json.toString())
    }

    fun getObject(key: String): JSONObject? {
        val str = getString(key) ?: return null
        return try {
            JSONObject(str)
        } catch (e: Exception) {
            null
        }
    }

    fun putList(key: String, list: List<String>) {
        val json = JSONArray(list)
        putString(key, json.toString())
    }

    fun getList(key: String): List<String>? {
        val str = getString(key) ?: return null
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
        cache.remove(key)
    }

    fun contains(key: String): Boolean {
        return cache.containsKey(key) || preferences.contains(key)
    }

    fun clear() {
        preferences.edit().clear().apply()
        cache.clear()
    }

    fun getAllKeys(): Set<String> {
        return preferences.all.keys
    }
}

object DataStoreKeys {
    const val REGISTRATION_TOKEN = "registration_token"
    const val REGISTRATION_EXPIRY = "registration_expiry"
    const val PHONE_NUMBER = "phone_number"
    const val IMS_CONFIG = "ims_config"
    const val CARRIER_CONFIG = "carrier_config"
    const val DEVICE_TOKEN = "device_token"
    const val LAST_SYNC_TIME = "last_sync_time"
    const val CAPABILITY_CACHE = "capability_cache"
    const val PENDING_MESSAGES = "pending_messages"
}
