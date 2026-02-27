/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.preference.PreferenceManager
import org.microg.gms.common.PackageUtils.warnIfNotMainProcess
import org.microg.gms.settings.SettingsContract.Auth
import org.microg.gms.settings.SettingsContract.CheckIn
import org.microg.gms.settings.SettingsContract.DroidGuard
import org.microg.gms.settings.SettingsContract.Exposure
import org.microg.gms.settings.SettingsContract.GameProfile
import org.microg.gms.settings.SettingsContract.Gcm
import org.microg.gms.settings.SettingsContract.Location
import org.microg.gms.settings.SettingsContract.Profile
import org.microg.gms.settings.SettingsContract.SafetyNet
import org.microg.gms.settings.SettingsContract.Vending
import org.microg.gms.settings.SettingsContract.WorkProfile
import org.microg.gms.settings.SettingsContract.getAuthority
import java.io.File


private const val SETTINGS_PREFIX = "org.microg.gms.settings."

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
    private val unifiedNlpPreferences by lazy {
        context!!.getSharedPreferences("unified_nlp", MODE_PRIVATE)
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
    private val metaDataPreferences: SharedPreferences by lazy {
        MetaDataPreferences(context!!, SETTINGS_PREFIX)
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
    ): Cursor? = when (uri.pathSegments.last()) {
        CheckIn.ID -> queryCheckIn(projection ?: CheckIn.PROJECTION)
        Gcm.ID -> queryGcm(projection ?: Gcm.PROJECTION)
        Auth.ID -> queryAuth(projection ?: Auth.PROJECTION)
        Exposure.ID -> queryExposure(projection ?: Exposure.PROJECTION)
        SafetyNet.ID -> querySafetyNet(projection ?: SafetyNet.PROJECTION)
        DroidGuard.ID -> queryDroidGuard(projection ?: DroidGuard.PROJECTION)
        Profile.ID -> queryProfile(projection ?: Profile.PROJECTION)
        Location.ID -> queryLocation(projection ?: Location.PROJECTION)
        Vending.ID -> queryVending(projection ?: Vending.PROJECTION)
        WorkProfile.ID -> queryWorkProfile(projection ?: WorkProfile.PROJECTION)
        GameProfile.ID -> queryGameProfile(projection ?: GameProfile.PROJECTION)
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
        when (uri.pathSegments.last()) {
            CheckIn.ID -> updateCheckIn(values)
            Gcm.ID -> updateGcm(values)
            Auth.ID -> updateAuth(values)
            Exposure.ID -> updateExposure(values)
            SafetyNet.ID -> updateSafetyNet(values)
            DroidGuard.ID -> updateDroidGuard(values)
            Profile.ID -> updateProfile(values)
            Location.ID -> updateLocation(values)
            Vending.ID -> updateVending(values)
            WorkProfile.ID -> updateWorkProfile(values)
            GameProfile.ID -> updateGameProfile(values)
            else -> return 0
        }
        return 1
    }

    private fun queryCheckIn(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            CheckIn.ENABLED -> getSettingsBoolean(key, false)
            CheckIn.ANDROID_ID -> checkInPrefs.getLong(key, 0)
            CheckIn.DIGEST -> checkInPrefs.getString(key, CheckIn.INITIAL_DIGEST)
                ?: CheckIn.INITIAL_DIGEST
            CheckIn.LAST_CHECK_IN -> checkInPrefs.getLong(key, 0)
            CheckIn.SECURITY_TOKEN -> checkInPrefs.getLong(key, 0)
            CheckIn.VERSION_INFO -> checkInPrefs.getString(key, "") ?: ""
            CheckIn.DEVICE_DATA_VERSION_INFO -> checkInPrefs.getString(key, "") ?: ""
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

    private fun queryGcm(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            Gcm.ENABLE_GCM -> getSettingsBoolean(key, false)
            Gcm.FULL_LOG -> getSettingsBoolean(key, true)
            Gcm.CONFIRM_NEW_APPS -> getSettingsBoolean(key, false)

            Gcm.LAST_PERSISTENT_ID -> preferences.getString(key, "") ?: ""

            Gcm.NETWORK_MOBILE -> Integer.parseInt(preferences.getString(key, "0") ?: "0")
            Gcm.NETWORK_WIFI -> Integer.parseInt(preferences.getString(key, "0") ?: "0")
            Gcm.NETWORK_ROAMING -> Integer.parseInt(preferences.getString(key, "0") ?: "0")
            Gcm.NETWORK_OTHER -> Integer.parseInt(preferences.getString(key, "0") ?: "0")

            Gcm.LEARNT_MOBILE -> preferences.getInt(key, 300000)
            Gcm.LEARNT_WIFI -> preferences.getInt(key, 300000)
            Gcm.LEARNT_OTHER -> preferences.getInt(key, 300000)

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
                Gcm.CONFIRM_NEW_APPS -> editor.putBoolean(key, value as Boolean)

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
            Auth.INCLUDE_ANDROID_ID -> getSettingsBoolean(key, true)
            Auth.STRIP_DEVICE_NAME -> getSettingsBoolean(key, false)
            Auth.TWO_STEP_VERIFICATION -> getSettingsBoolean(key, false)
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
                Auth.INCLUDE_ANDROID_ID -> editor.putBoolean(key, value as Boolean)
                Auth.STRIP_DEVICE_NAME -> editor.putBoolean(key, value as Boolean)
                Auth.TWO_STEP_VERIFICATION -> editor.putBoolean(key, value as Boolean)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun queryExposure(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            Exposure.SCANNER_ENABLED -> getSettingsBoolean(key, false)
            Exposure.LAST_CLEANUP -> preferences.getLong(key, 0L)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateExposure(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                Exposure.SCANNER_ENABLED -> editor.putBoolean(key, value as Boolean)
                Exposure.LAST_CLEANUP -> editor.putLong(key, value as Long)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun querySafetyNet(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            SafetyNet.ENABLED -> getSettingsBoolean(key, false)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateSafetyNet(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                SafetyNet.ENABLED -> editor.putBoolean(key, value as Boolean)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun queryDroidGuard(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            DroidGuard.ENABLED -> getSettingsBoolean(key, false)
            DroidGuard.MODE -> getSettingsString(key)
            DroidGuard.NETWORK_SERVER_URL -> getSettingsString(key)
            DroidGuard.FORCE_LOCAL_DISABLED -> systemDefaultPreferences?.getBoolean(key, false) ?: false
            DroidGuard.HARDWARE_ATTESTATION_BLOCKED -> getSettingsBoolean(key, true)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateDroidGuard(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                DroidGuard.ENABLED -> editor.putBoolean(key, value as Boolean)
                DroidGuard.MODE -> editor.putString(key, value as String)
                DroidGuard.NETWORK_SERVER_URL -> editor.putString(key, value as String)
                DroidGuard.HARDWARE_ATTESTATION_BLOCKED -> editor.putBoolean(key, value as Boolean)
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

    private fun queryLocation(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            Location.WIFI_ICHNAEA -> getSettingsBoolean(key, hasUnifiedNlpLocationBackend("org.microg.nlp.backend.ichnaea"))
            Location.WIFI_MOVING -> getSettingsBoolean(key, hasUnifiedNlpLocationBackend("de.sorunome.unifiednlp.trains"))
            Location.WIFI_LEARNING -> getSettingsBoolean(key, false)
            Location.WIFI_CACHING -> getSettingsBoolean(key, getSettingsBoolean(Location.WIFI_LEARNING, false) == 1)
            Location.CELL_ICHNAEA -> getSettingsBoolean(key, hasUnifiedNlpLocationBackend("org.microg.nlp.backend.ichnaea"))
            Location.CELL_LEARNING -> getSettingsBoolean(key, true)
            Location.CELL_CACHING -> getSettingsBoolean(key, getSettingsBoolean(Location.CELL_LEARNING, true) == 1)
            Location.GEOCODER_NOMINATIM -> getSettingsBoolean(key, hasUnifiedNlpGeocoderBackend("org.microg.nlp.backend.nominatim") )
            Location.ICHNAEA_ENDPOINT -> getSettingsString(key, null)
            Location.ONLINE_SOURCE -> getSettingsString(key, null)
            Location.ICHNAEA_CONTRIBUTE -> getSettingsBoolean(key, false)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }
    private fun hasUnifiedNlpPrefixInStringSet(key: String, vararg prefixes: String) = getUnifiedNlpSettingsStringSetCompat(key, emptySet()).any { entry -> prefixes.any { prefix -> entry.startsWith(prefix)}}
    private fun hasUnifiedNlpLocationBackend(vararg packageNames: String) = hasUnifiedNlpPrefixInStringSet("location_backends", *packageNames.map { "$it/" }.toTypedArray())
    private fun hasUnifiedNlpGeocoderBackend(vararg packageNames: String) = hasUnifiedNlpPrefixInStringSet("geocoder_backends", *packageNames.map { "$it/" }.toTypedArray())

    private fun updateLocation(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                Location.WIFI_ICHNAEA -> editor.putBoolean(key, value as Boolean)
                Location.WIFI_MOVING -> editor.putBoolean(key, value as Boolean)
                Location.WIFI_LEARNING -> editor.putBoolean(key, value as Boolean)
                Location.CELL_ICHNAEA -> editor.putBoolean(key, value as Boolean)
                Location.CELL_LEARNING -> editor.putBoolean(key, value as Boolean)
                Location.GEOCODER_NOMINATIM -> editor.putBoolean(key, value as Boolean)
                Location.ICHNAEA_ENDPOINT -> (value as String).let { if (it.isBlank()) editor.remove(key) else editor.putString(key, it) }
                Location.ONLINE_SOURCE -> (value as? String?).let { if (it.isNullOrBlank()) editor.remove(key) else editor.putString(key, it) }
                Location.ICHNAEA_CONTRIBUTE -> editor.putBoolean(key, value as Boolean)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun queryVending(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            Vending.LICENSING -> getSettingsBoolean(key, false)
            Vending.LICENSING_PURCHASE_FREE_APPS -> getSettingsBoolean(key, false)
            Vending.BILLING -> getSettingsBoolean(key, false)
            Vending.ASSET_DELIVERY -> getSettingsBoolean(key, false)
            Vending.ASSET_DEVICE_SYNC -> getSettingsBoolean(key, false)
            Vending.SPLIT_INSTALL -> getSettingsBoolean(key, false)
            Vending.APPS_INSTALL -> getSettingsBoolean(key, false)
            Vending.APPS_INSTALLER_LIST -> getSettingsString(key, "")
            Vending.PLAY_INTEGRITY_APP_LIST -> getSettingsString(key, "")
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateVending(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                Vending.LICENSING -> editor.putBoolean(key, value as Boolean)
                Vending.LICENSING_PURCHASE_FREE_APPS -> editor.putBoolean(key, value as Boolean)
                Vending.BILLING -> editor.putBoolean(key, value as Boolean)
                Vending.SPLIT_INSTALL -> editor.putBoolean(key, value as Boolean)
                Vending.ASSET_DELIVERY -> editor.putBoolean(key, value as Boolean)
                Vending.ASSET_DEVICE_SYNC -> editor.putBoolean(key, value as Boolean)
                Vending.APPS_INSTALL -> editor.putBoolean(key, value as Boolean)
                Vending.APPS_INSTALLER_LIST -> editor.putString(key, value as String)
                Vending.PLAY_INTEGRITY_APP_LIST -> editor.putString(key, value as String)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun queryWorkProfile(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            WorkProfile.CREATE_WORK_ACCOUNT -> getSettingsBoolean(key, false)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateWorkProfile(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                WorkProfile.CREATE_WORK_ACCOUNT -> editor.putBoolean(key, value as Boolean)
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
        editor.apply()
    }

    private fun queryGameProfile(p: Array<out String>): Cursor = MatrixCursor(p).addRow(p) { key ->
        when (key) {
            GameProfile.ALLOW_CREATE_PLAYER -> getSettingsBoolean(key, false)
            GameProfile.ALLOW_UPLOAD_GAME_PLAYED -> getSettingsBoolean(key, false)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    private fun updateGameProfile(values: ContentValues) {
        if (values.size() == 0) return
        val editor = preferences.edit()
        values.valueSet().forEach { (key, value) ->
            when (key) {
                GameProfile.ALLOW_CREATE_PLAYER -> editor.putBoolean(key, value as Boolean)
                GameProfile.ALLOW_UPLOAD_GAME_PLAYED -> editor.putBoolean(key, value as Boolean)
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
        return listOf(preferences, systemDefaultPreferences, metaDataPreferences).getBooleanAsInt(key, def)
    }

    private fun getSettingsString(key: String, def: String? = null): String? = listOf(preferences, systemDefaultPreferences, metaDataPreferences).getString(key, def)
    private fun getSettingsInt(key: String, def: Int): Int = listOf(preferences, systemDefaultPreferences, metaDataPreferences).getInt(key, def)
    private fun getSettingsLong(key: String, def: Long): Long = listOf(preferences, systemDefaultPreferences, metaDataPreferences).getLong(key, def)
    private fun getUnifiedNlpSettingsStringSetCompat(key: String, def: Set<String>): Set<String> = listOf(unifiedNlpPreferences, preferences, systemDefaultPreferences).getStringSetCompat(key, def)

    private fun SharedPreferences.getStringSetCompat(key: String, def: Set<String>): Set<String> {
        if (SDK_INT >= 11) {
            try {
                val res = getStringSet(key, null)
                if (res != null) return res.filter { it.isNotEmpty() }.toSet()
            } catch (ignored: Exception) {
                // Ignore
            }
        }
        try {
            val str = getString(key, null)
            if (str != null) return str.split("\\|".toRegex()).filter { it.isNotEmpty() }.toSet()
        } catch (ignored: Exception) {
            // Ignore
        }
        return def
    }

    private fun List<SharedPreferences?>.getStringSetCompat(key: String, def: Set<String>): Set<String> = foldRight(def) { preferences, defValue -> preferences?.getStringSetCompat(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getString(key: String, def: String?): String? = foldRight(def) { preferences, defValue -> preferences?.getString(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getInt(key: String, def: Int): Int = foldRight(def) { preferences, defValue -> preferences?.getInt(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getLong(key: String, def: Long): Long = foldRight(def) { preferences, defValue -> preferences?.getLong(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getBoolean(key: String, def: Boolean): Boolean = foldRight(def) { preferences, defValue -> preferences?.getBoolean(key, defValue) ?: defValue }
    private fun List<SharedPreferences?>.getBooleanAsInt(key: String, def: Boolean): Int = if (getBoolean(key, def)) 1 else 0
}
