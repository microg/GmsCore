/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsConfigManager - Runtime configuration management
 */

package org.microg.gms.rcs.config

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class RcsConfigManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "rcs_config"
        
        @Volatile
        private var instance: RcsConfigManager? = null
        
        fun getInstance(context: Context): RcsConfigManager {
            return instance ?: synchronized(this) {
                instance ?: RcsConfigManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val preferences: SharedPreferences
    private val configCache = ConcurrentHashMap<String, Any>()
    private val changeListeners = mutableListOf<ConfigChangeListener>()

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
        
        loadDefaults()
    }

    private fun loadDefaults() {
        setDefault(ConfigKeys.RCS_ENABLED, true)
        setDefault(ConfigKeys.AUTO_DOWNLOAD_FILES, true)
        setDefault(ConfigKeys.MAX_FILE_SIZE_MB, 100)
        setDefault(ConfigKeys.SEND_READ_RECEIPTS, true)
        setDefault(ConfigKeys.SEND_TYPING_INDICATORS, true)
        setDefault(ConfigKeys.SHOW_PRESENCE, true)
        setDefault(ConfigKeys.AUTO_RECONNECT, true)
        setDefault(ConfigKeys.MAX_RECONNECT_ATTEMPTS, 10)
        setDefault(ConfigKeys.CONNECTION_TIMEOUT_SECONDS, 30)
        setDefault(ConfigKeys.MESSAGE_RETRY_COUNT, 3)
        setDefault(ConfigKeys.ENABLE_DEBUG_LOGGING, false)
        setDefault(ConfigKeys.ENABLE_ANALYTICS, false)
    }

    private fun setDefault(key: String, value: Any) {
        if (!preferences.contains(key)) {
            set(key, value)
        }
    }

    fun getString(key: String, default: String = ""): String {
        return configCache.getOrPut(key) {
            preferences.getString(key, default) ?: default
        } as String
    }

    fun getInt(key: String, default: Int = 0): Int {
        return configCache.getOrPut(key) {
            preferences.getInt(key, default)
        } as Int
    }

    fun getLong(key: String, default: Long = 0L): Long {
        return configCache.getOrPut(key) {
            preferences.getLong(key, default)
        } as Long
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return configCache.getOrPut(key) {
            preferences.getBoolean(key, default)
        } as Boolean
    }

    fun set(key: String, value: Any) {
        val editor = preferences.edit()
        
        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            else -> editor.putString(key, value.toString())
        }
        
        editor.apply()
        configCache[key] = value
        notifyChange(key, value)
    }

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
        configCache.remove(key)
        notifyChange(key, null)
    }

    fun exportConfig(): String {
        val json = JSONObject()
        preferences.all.forEach { (key, value) ->
            json.put(key, value)
        }
        return json.toString(2)
    }

    fun importConfig(jsonString: String) {
        val json = JSONObject(jsonString)
        val editor = preferences.edit()
        
        json.keys().forEach { key ->
            when (val value = json.get(key)) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
            }
        }
        
        editor.apply()
        configCache.clear()
    }

    fun addChangeListener(listener: ConfigChangeListener) {
        changeListeners.add(listener)
    }

    fun removeChangeListener(listener: ConfigChangeListener) {
        changeListeners.remove(listener)
    }

    private fun notifyChange(key: String, value: Any?) {
        changeListeners.forEach { it.onConfigChanged(key, value) }
    }

    fun resetToDefaults() {
        preferences.edit().clear().apply()
        configCache.clear()
        loadDefaults()
    }
}

object ConfigKeys {
    const val RCS_ENABLED = "rcs_enabled"
    const val AUTO_DOWNLOAD_FILES = "auto_download_files"
    const val MAX_FILE_SIZE_MB = "max_file_size_mb"
    const val SEND_READ_RECEIPTS = "send_read_receipts"
    const val SEND_TYPING_INDICATORS = "send_typing_indicators"
    const val SHOW_PRESENCE = "show_presence"
    const val AUTO_RECONNECT = "auto_reconnect"
    const val MAX_RECONNECT_ATTEMPTS = "max_reconnect_attempts"
    const val CONNECTION_TIMEOUT_SECONDS = "connection_timeout_seconds"
    const val MESSAGE_RETRY_COUNT = "message_retry_count"
    const val ENABLE_DEBUG_LOGGING = "enable_debug_logging"
    const val ENABLE_ANALYTICS = "enable_analytics"
    const val CARRIER_CONFIG_JSON = "carrier_config_json"
    const val LAST_REGISTRATION_TIME = "last_registration_time"
    const val REGISTRATION_EXPIRY_TIME = "registration_expiry_time"
}

interface ConfigChangeListener {
    fun onConfigChanged(key: String, value: Any?)
}
