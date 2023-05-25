package org.microg.gms.gcm

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import org.microg.gms.gcm.TriggerReceiver.FORCE_TRY_RECONNECT
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.Gcm
import org.microg.gms.settings.SettingsContract.setSettings
import kotlin.math.max
import kotlin.math.min

data class GcmPrefs(
    val isGcmLogEnabled: Boolean,
    val lastPersistedId: String?,
    val confirmNewApps: Boolean,
    val gcmEnabled: Boolean,
    val networkMobile: Int,
    val networkWifi: Int,
    val networkRoaming: Int,
    val networkOther: Int,
    val learntMobileInterval: Int,
    val learntWifiInterval: Int,
    val learntOtherInterval: Int,
) {

    val isEnabled: Boolean get() = gcmEnabled

    val lastPersistedIds: List<String>
        get() = if (lastPersistedId.isNullOrEmpty()) emptyList() else lastPersistedId.split("\\|")

    companion object {
        const val PREF_CONFIRM_NEW_APPS = Gcm.CONFIRM_NEW_APPS
        const val PREF_NETWORK_MOBILE = Gcm.NETWORK_MOBILE
        const val PREF_NETWORK_WIFI = Gcm.NETWORK_WIFI
        const val PREF_NETWORK_ROAMING = Gcm.NETWORK_ROAMING
        const val PREF_NETWORK_OTHER = Gcm.NETWORK_OTHER

        private const val MIN_INTERVAL = 5 * 60 * 1000 // 5 minutes
        private const val MAX_INTERVAL = 30 * 60 * 1000 // 30 minutes

        @JvmStatic
        fun get(context: Context): GcmPrefs {
            return SettingsContract.getSettings(context, Gcm.getContentUri(context), Gcm.PROJECTION) { c ->
                GcmPrefs(
                    isGcmLogEnabled = c.getInt(0) != 0,
                    lastPersistedId = c.getString(1),
                    confirmNewApps = c.getInt(2) != 0,
                    gcmEnabled = c.getInt(3) != 0,
                    networkMobile = c.getInt(4),
                    networkWifi = c.getInt(5),
                    networkRoaming = c.getInt(6),
                    networkOther = c.getInt(7),
                    learntMobileInterval = c.getInt(8),
                    learntWifiInterval = c.getInt(9),
                    learntOtherInterval = c.getInt(10),
                )
            }
        }

        fun write(context: Context, config: ServiceConfiguration) {
            val gcmPrefs = get(context)
            setSettings(context, Gcm.getContentUri(context)) {
                put(Gcm.ENABLE_GCM, config.enabled)
                put(Gcm.CONFIRM_NEW_APPS, config.confirmNewApps)
                put(Gcm.NETWORK_MOBILE, config.mobile)
                put(Gcm.NETWORK_WIFI, config.wifi)
                put(Gcm.NETWORK_ROAMING, config.roaming)
                put(Gcm.NETWORK_OTHER, config.other)
            }
            gcmPrefs.setEnabled(context, config.enabled)
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            val prefs = get(context)
            setSettings(context, Gcm.getContentUri(context)) {
                put(Gcm.ENABLE_GCM, enabled)
            }
            prefs.setEnabled(context, enabled)
        }

        @JvmStatic
        fun clearLastPersistedId(context: Context) {
            setSettings(context, Gcm.getContentUri(context)) {
                put(Gcm.LAST_PERSISTENT_ID, "")
            }
        }
    }

    /**
     * Call this whenever the enabled state of GCM has changed.
     */
    private fun setEnabled(context: Context, enabled: Boolean) {
        if (gcmEnabled == enabled) return
        if (enabled) {
            val i = Intent(FORCE_TRY_RECONNECT, null, context, TriggerReceiver::class.java)
            context.sendBroadcast(i)
        } else {
            McsService.stop(context)
        }
    }

    @Suppress("DEPRECATION")
    fun getNetworkPrefForInfo(info: NetworkInfo?): String {
        if (info == null) return PREF_NETWORK_OTHER
        return if (info.isRoaming) PREF_NETWORK_ROAMING else when (info.type) {
            ConnectivityManager.TYPE_MOBILE -> PREF_NETWORK_MOBILE
            ConnectivityManager.TYPE_WIFI -> PREF_NETWORK_WIFI
            else -> PREF_NETWORK_OTHER
        }
    }

    @Suppress("DEPRECATION")
    fun getHeartbeatMsFor(info: NetworkInfo?): Int {
        return getHeartbeatMsFor(getNetworkPrefForInfo(info))
    }

    fun getHeartbeatMsFor(pref: String): Int {
        return if (PREF_NETWORK_ROAMING == pref) {
            if (networkRoaming != 0) networkRoaming * 60000 else learntMobileInterval
        } else if (PREF_NETWORK_MOBILE == pref) {
            if (networkMobile != 0) networkMobile * 60000 else learntMobileInterval
        } else if (PREF_NETWORK_WIFI == pref) {
            if (networkWifi != 0) networkWifi * 60000 else learntWifiInterval
        } else {
            if (networkOther != 0) networkOther * 60000 else learntOtherInterval
        }
    }

    fun learnTimeout(context: Context, pref: String) {
        Log.d("GmsGcmPrefs", "learnTimeout: $pref")
        when (pref) {
            PREF_NETWORK_MOBILE, PREF_NETWORK_ROAMING -> setSettings(context, Gcm.getContentUri(context)) {
                val newInterval = (learntMobileInterval * 0.95).toInt()
                put(Gcm.LEARNT_MOBILE, max(MIN_INTERVAL, min(newInterval, MAX_INTERVAL)))
            }
            PREF_NETWORK_WIFI -> setSettings(context, Gcm.getContentUri(context)) {
                val newInterval = (learntWifiInterval * 0.95).toInt()
                put(Gcm.LEARNT_WIFI, max(MIN_INTERVAL, min(newInterval, MAX_INTERVAL)))
            }
            else -> setSettings(context, Gcm.getContentUri(context)) {
                val newInterval = (learntOtherInterval * 0.95).toInt()
                put(Gcm.LEARNT_OTHER, max(MIN_INTERVAL, min(newInterval, MAX_INTERVAL)))
            }
        }
    }

    fun learnReached(context: Context, pref: String, time: Long) {
        Log.d("GmsGcmPrefs", "learnReached: $pref / $time")
        when (pref) {
            PREF_NETWORK_MOBILE, PREF_NETWORK_ROAMING -> {
                if (time > learntMobileInterval / 4 * 3) {
                    val newInterval = (learntMobileInterval * 1.02).toInt()
                    setSettings(context, Gcm.getContentUri(context)) {
                        put(Gcm.LEARNT_MOBILE, max(MIN_INTERVAL, min(newInterval, MAX_INTERVAL)))
                    }
                }
            }
            PREF_NETWORK_WIFI -> {
                if (time > learntWifiInterval / 4 * 3) {
                    val newInterval = (learntWifiInterval * 1.02).toInt()
                    setSettings(context, Gcm.getContentUri(context)) {
                        put(Gcm.LEARNT_WIFI, max(MIN_INTERVAL, min(newInterval, MAX_INTERVAL)))
                    }
                }
            }
            else -> {
                if (time > learntOtherInterval / 4 * 3) {
                    val newInterval = (learntOtherInterval * 1.02).toInt()
                    setSettings(context, Gcm.getContentUri(context)) {
                        put(Gcm.LEARNT_OTHER, max(MIN_INTERVAL, min(newInterval, MAX_INTERVAL)))
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun isEnabledFor(info: NetworkInfo?): Boolean {
        return isEnabled && info != null && getHeartbeatMsFor(info) >= 0
    }

    fun extendLastPersistedId(context: Context, id: String) {
        val newId = if (lastPersistedId.isNullOrEmpty()) id else "$lastPersistedId|$id"
        setSettings(context, Gcm.getContentUri(context)) {
            put(Gcm.LAST_PERSISTENT_ID, newId)
        }
    }

}
