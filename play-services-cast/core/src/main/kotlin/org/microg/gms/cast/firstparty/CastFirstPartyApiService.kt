/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.firstparty

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.cast.firstparty.internal.ICastFirstPartyService
import com.google.android.gms.cast.firstparty.internal.ICastSettingsCallback
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "CastFirstPartyApi"

private const val PREFS_NAME = "google_cast"
private const val KEY_CAST_ENABLED = "googlecast-isEnabled"
private const val KEY_RELAY_CASTING_ENABLED = "SETTINGS_RELAY_CASTING_ENABLED"
private const val KEY_CAST_ENABLED_STATUS = "com.google.android.gms.cast.BUNDLE_KEY_CAST_ENABLED_STATUS"
private const val KEY_NEARBY_TOGGLE_STATE = "com.google.android.gms.cast.SETTINGS_KEY_CAST_NEARBY_TOGGLE_STATE"
private const val KEY_NEARBY_TOGGLE_TIMESTAMP = "com.google.android.gms.cast.SETTINGS_KEY_CAST_NEARBY_TOGGLE_TIMESTAMP"
private const val KEY_ACTIVE_ACCOUNT = "RELAY_CASTING_ACTIVE_ACCOUNT_NAME"

/** Features advertised by the Cast First Party service. */
private val FEATURES = arrayOf(
    Feature("client_side_logging", 1L, true),
    Feature("cxless_client_minimal", 1L, true),
    Feature("cxless_caf_control", 1L, true),
    Feature("module_flag_control", 1L, true),
    Feature("discovery_hint_supply", 1L, true),
    Feature("relay_casting_set_active_account", 1L, true),
    Feature("analytics_proto_enum_translation", 1L, true),
    Feature("integer_to_integer_map", 1L, true),
    Feature("relay_casting_set_remote_casting_mode", 1L, true),
    Feature("get_relay_access_token", 1L, true),
    Feature("get_cast_settings", 1L, true),
    Feature("set_bundle_setting", 1L, true),
    Feature("get_client_updated_info", 1L, true),
    Feature("device_suggestions", 1L, true),
    Feature("cxless_connect_with_options", 1L, true),
)

class CastFirstPartyApiService : BaseService(TAG, GmsService.CAST_FIRST_PARTY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest")
        val requestedPackage = request.packageName
        try {
            PackageUtils.getAndCheckCallingPackage(this, requestedPackage)
                ?: throw IllegalArgumentException("Missing calling package")
            callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS,
                CastFirstPartyServiceImpl(this),
                ConnectionInfo().apply { features = FEATURES },
            )
        } catch (_: Exception) {
            Log.w(TAG, "Service request rejected")
            callback.onPostInitComplete(CommonStatusCodes.INTERNAL_ERROR, null, null)
        }
    }
}

private class CastFirstPartyServiceImpl(context: Context) : ICastFirstPartyService.Stub() {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getCastSettings(callback: ICastSettingsCallback?, requestMetadata: ApiMetadata?) {
        Log.d(TAG, "getCastSettings")
        if (callback == null) {
            Log.w(TAG, "getCastSettings called without a callback")
            return
        }

        val settings = Bundle().apply {
            val isLowRamDevice = (appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                ?.isLowRamDevice == true
            val defaultCastEnabled = !isLowRamDevice
            putBoolean(KEY_CAST_ENABLED, preferences.getBoolean(KEY_CAST_ENABLED, defaultCastEnabled))
            putBoolean(KEY_RELAY_CASTING_ENABLED, true)
            putInt(KEY_CAST_ENABLED_STATUS, 1)
            putBoolean(KEY_NEARBY_TOGGLE_STATE, preferences.getBoolean(KEY_NEARBY_TOGGLE_STATE, false))
            putLong(KEY_NEARBY_TOGGLE_TIMESTAMP, preferences.getLong(KEY_NEARBY_TOGGLE_TIMESTAMP, 0L))
        }
        runCatching {
            callback.onCastSettings(settings, ApiMetadata.DEFAULT)
        }.onFailure { Log.w(TAG, "Failed to deliver Cast settings", it) }
    }

    override fun setBooleanSetting(key: String?, value: Boolean, metadata: ApiMetadata?) {
        Log.d(TAG, "setBooleanSetting")
        if (key == null) {
            Log.w(TAG, "setBooleanSetting called without a key")
            return
        }
        preferences.edit().putBoolean(key, value).apply()
    }

    override fun setActiveAccount(accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "setActiveAccount")
        preferences.edit().putString(KEY_ACTIVE_ACCOUNT, accountName).apply()
    }

    override fun setBundleSettings(
        keys: List<String?>?,
        types: List<*>?,
        values: Bundle?,
        metadata: ApiMetadata?,
    ) {
        Log.d(TAG, "setBundleSettings")
        val safeKeys = keys.orEmpty()
        val safeTypes = types.orEmpty()
        val safeValues = values ?: Bundle.EMPTY

        if (safeKeys.size != safeTypes.size) {
            Log.w(TAG, "setBundleSettings called with mismatched key and type counts")
            return
        }

        val editor = preferences.edit()
        safeKeys.forEachIndexed { index, key ->
            if (key == null) return@forEachIndexed
            when (safeTypes[index] as? Int) {
                0 -> editor.putBoolean(key, safeValues.getBoolean(key))
                1 -> editor.putLong(key, safeValues.getLong(key))
                2 -> editor.putString(key, safeValues.getString(key))
                else -> Log.w(TAG, "setBundleSettings ignored an unsupported value type")
            }
        }
        editor.apply()
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
