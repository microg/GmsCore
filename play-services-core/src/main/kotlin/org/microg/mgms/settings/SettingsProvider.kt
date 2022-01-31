/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
@file:Suppress("DEPRECATION")

package org.microg.mgms.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.preference.PreferenceManager
import org.microg.gms.common.PackageUtils.warnIfNotMainProcess
import org.microg.gms.gcm.GcmPrefs
import org.microg.mgms.settings.SettingsContract.Auth
import org.microg.mgms.settings.SettingsContract.CheckIn
import org.microg.mgms.settings.SettingsContract.Gcm
import org.microg.mgms.settings.SettingsContract.Profile
import org.microg.mgms.settings.SettingsContract.getAuthority
import java.io.File

/**
 * All settings access should go through this [ContentProvider],
 * because it provides safe access from different processes which normal [SharedPreferences] don't.
 */
class SettingsProvider : ContentProvider() {

    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    private val checkInPrefs by lazy {
        context!!.getSharedPreferences(CheckIn.PREFERENCES_NAME, MODE_PRIVATE)
    }
    private val systemDefaultPreferences: SharedPreferences? by lazy {
        try {
            Context::class.java.getDeclaredMethod(
                "getSharedPreferences",
                File::class.java,
                Int::class.javaPrimitiveType
            ).invoke(context, File("/system/etc/microg.xml"), MODE_PRIVATE) as SharedPreferences
        } catch (ignored: Exception) {
            null
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = when (uri) {
        CheckIn.getContentUri(context!!) -> queryCheckIn(projection ?: CheckIn.PROJECTION)
        Gcm.getContentUri(context!!) -> queryGcm(projection ?: Gcm.PROJECTION)
        Auth.getContentUri(context!!) -> queryAuth(projection ?: Auth.PROJECTION)
        Profile.getContentUri(context!!) -> queryProfile(projection ?: Profile.PROJECTION)
        else -> null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        warnIfNotMainProcess(context, this.javaClass)
        if (values == null) return 0
        when (uri) {
            CheckIn.getContentUri(context!!) -> updateCheckIn(values)
            Gcm.getContentUri(context!!) -> updateGcm(values)
            Auth.getContentUri(context!!) -> updateAuth(values)
            Profile.getContentUri(context!!) -> updateProfile(values)
            else -> return 0
        }
        return 1
    }

    private fun queryCheckIn(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            CheckIn.ENABLED -> getSettingsBoolean(key, true)
            CheckIn.ANDROID_ID -> checkInPrefs.getLong(key, 0)
            CheckIn.DIGEST -> checkInPrefs.getString(key, CheckIn.INITIAL_DIGEST)
                ?: CheckIn.INITIAL_DIGEST
            CheckIn.LAST_CHECK_IN -> checkInPrefs.getLong(key, 0)
            CheckIn.SECURITY_TOKEN -> checkInPrefs.getLong(key, 0)
            CheckIn.VERSION_INFO -> checkInPrefs.getString(key, "") ?: ""
            CheckIn.DEVICE_DATA_VERSION_INFO -> checkInPrefs.getString(key, "") ?: ""
            CheckIn.BRAND_SPOOF -> getSettingsBoolean(key, false)
            CheckIn.HIDE_LAUNCHER_ICON -> getSettingsBoolean(key, false)
            else -> throw IllegalArgumentException()
        }
    }

    private fun updateCheckIn(values: ContentValues) {
        if (values.size() == 0) return
        if (values.size() == 1 && values.containsKey(CheckIn.ENABLED)) {
            // special case: only changing enabled state
            updateCheckInEnabled(values.getAsBoolean(CheckIn.ENABLED))
            return
        }
        val editor = checkInPrefs.edit()
        values.valueSet().forEach { (key, value) ->
            if (key == CheckIn.ENABLED) {
                // special case: not saved in checkInPrefs
                updateCheckInEnabled(value as Boolean)
            }
            if (key == CheckIn.BRAND_SPOOF) {
                // special case: not saved in checkInPrefs
                updateSpoofingEnabled(value as Boolean)
            }
            if (key == CheckIn.HIDE_LAUNCHER_ICON) {
                // special case: not saved in checkInPrefs
                updateHideLauncherIcon(value as Boolean)
            }
            when (key) {
                CheckIn.ANDROID_ID -> editor.putLong(key, value as Long)
                CheckIn.DIGEST -> editor.putString(key, value as String?)
                CheckIn.LAST_CHECK_IN -> editor.putLong(key, value as Long)
                CheckIn.SECURITY_TOKEN -> editor.putLong(key, value as Long)
                CheckIn.VERSION_INFO -> editor.putString(key, value as String?)
                CheckIn.DEVICE_DATA_VERSION_INFO -> editor.putString(key, value as String?)
            }
        }
        editor.apply()
    }

    private fun updateCheckInEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(CheckIn.ENABLED, enabled)
            .apply()
    }

    private fun updateSpoofingEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(CheckIn.BRAND_SPOOF, enabled)
            .apply()
    }

    private fun updateHideLauncherIcon(enabled: Boolean) {
        preferences.edit()
                .putBoolean(CheckIn.HIDE_LAUNCHER_ICON, enabled)
                .apply()
    }

    private fun queryGcm(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            Gcm.ENABLE_GCM -> getSettingsBoolean(key, true)
            Gcm.FULL_LOG -> getSettingsBoolean(key, true)

            Gcm.LAST_PERSISTENT_ID -> preferences.getString(key, "") ?: ""

            Gcm.NETWORK_MOBILE -> Integer.parseInt(preferences.getString(key, "0") ?: "0")
            Gcm.NETWORK_WIFI -> Integer.parseInt(preferences.getString(key, "0") ?: "0")
            Gcm.NETWORK_ROAMING -> Integer.parseInt(preferences.getString(key, "0") ?: "0")
            Gcm.NETWORK_OTHER -> Integer.parseInt(preferences.getString(key, "0") ?: "0")

            Gcm.LEARNT_MOBILE -> preferences.getInt(key, GcmPrefs.INTERVAL)
            Gcm.LEARNT_WIFI -> preferences.getInt(key, GcmPrefs.INTERVAL)
            Gcm.LEARNT_OTHER -> preferences.getInt(key, GcmPrefs.INTERVAL)

            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateGcm(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                Gcm.ENABLE_GCM -> editor.putBoolean(key, value as Boolean)
                Gcm.FULL_LOG -> editor.putBoolean(key, value as Boolean)

                Gcm.LAST_PERSISTENT_ID -> editor.putString(key, value as String?)

                Gcm.NETWORK_MOBILE -> editor.putString(key, (value as Int).toString())
                Gcm.NETWORK_WIFI -> editor.putString(key, (value as Int).toString())
                Gcm.NETWORK_ROAMING -> editor.putString(key, (value as Int).toString())
                Gcm.NETWORK_OTHER -> editor.putString(key, (value as Int).toString())

                Gcm.LEARNT_MOBILE -> editor.putInt(key, value as Int)
                Gcm.LEARNT_WIFI -> editor.putInt(key, value as Int)
                Gcm.LEARNT_OTHER -> editor.putInt(key, value as Int)

                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun queryAuth(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            Auth.TRUST_GOOGLE -> getSettingsBoolean(key, true)
            Auth.VISIBLE -> getSettingsBoolean(key, false)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateAuth(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                Auth.TRUST_GOOGLE -> editor.putBoolean(key, value as Boolean)
                Auth.VISIBLE -> editor.putBoolean(key, value as Boolean)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun queryProfile(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            Profile.PROFILE -> getSettingsString(key, "auto")
            Profile.SERIAL -> getSettingsString(key)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateProfile(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                Profile.PROFILE -> editor.putString(key, value as String?)
                Profile.SERIAL -> editor.putString(key, value as String?)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun MatrixCursor.addRow(
        p: Array<out String>,
        valueGetter: (String) -> Any?
    ): MatrixCursor {
        val row = newRow()
        for (key in p) row.add(valueGetter.invoke(key))
        return this
    }

    override fun getType(uri: Uri): String {
        return "vnd.android.cursor.item/vnd.${getAuthority(context!!)}.${uri.path}"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    /**
     * Returns the current setting of the given [key]
     * using the default value from [systemDefaultPreferences] or [def] if not available.
     * @return the current setting as [Int], because [ContentProvider] does not support [Boolean].
     */
    private fun getSettingsBoolean(key: String, def: Boolean): Int {
        return listOf(preferences, systemDefaultPreferences).getBooleanAsInt(key, def)
    }

    private fun getSettingsString(key: String, def: String? = null): String? = listOf(preferences, systemDefaultPreferences).getString(key, def)
    private fun getSettingsInt(key: String, def: Int): Int = listOf(preferences, systemDefaultPreferences).getInt(key, def)
    private fun getSettingsLong(key: String, def: Long): Long = listOf(preferences, systemDefaultPreferences).getLong(key, def)

    private fun List<SharedPreferences?>.getString(key: String, def: String?): String? = foldRight(def) { preferences, defValue -> preferences?.getString(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getInt(key: String, def: Int): Int = foldRight(def) { preferences, defValue -> preferences?.getInt(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getLong(key: String, def: Long): Long = foldRight(def) { preferences, defValue -> preferences?.getLong(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getBoolean(key: String, def: Boolean): Boolean = foldRight(def) { preferences, defValue -> preferences?.getBoolean(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getBooleanAsInt(key: String, def: Boolean): Int = if (getBoolean(key, def)) 1 else 0
}
