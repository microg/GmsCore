package org.microg.gms.settings

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

object SettingsContract {
    const val AUTHORITY = "org.microg.gms.settings"
    val AUTHORITY_URI: Uri = Uri.parse("content://$AUTHORITY")

    object CheckIn {
        private const val id = "check-in"
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, id)
        const val CONTENT_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$id"

        const val ENABLED = "checkin_enable_service"
        const val ANDROID_ID = "androidId"
        const val DIGEST = "digest"
        const val LAST_CHECK_IN = "lastCheckin"
        const val SECURITY_TOKEN = "securityToken"
        const val VERSION_INFO = "versionInfo"
        const val DEVICE_DATA_VERSION_INFO = "deviceDataVersionInfo"

        val PROJECTION = arrayOf(
            ENABLED,
            ANDROID_ID,
            DIGEST,
            LAST_CHECK_IN,
            SECURITY_TOKEN,
            VERSION_INFO,
            DEVICE_DATA_VERSION_INFO,
        )
        const val PREFERENCES_NAME = "checkin"
        const val INITIAL_DIGEST = "1-929a0dca0eee55513280171a8585da7dcd3700f8"
    }

    object Gcm {
        private const val id = "gcm"
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, id)
        const val CONTENT_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$id"

        const val FULL_LOG = "gcm_full_log"
        const val LAST_PERSISTENT_ID = "gcm_last_persistent_id"
        const val CONFIRM_NEW_APPS = "gcm_confirm_new_apps"
        const val ENABLE_GCM = "gcm_enable_mcs_service"

        const val NETWORK_MOBILE = "gcm_network_mobile"
        const val NETWORK_WIFI = "gcm_network_wifi"
        const val NETWORK_ROAMING = "gcm_network_roaming"
        const val NETWORK_OTHER = "gcm_network_other"

        const val LEARNT_MOBILE = "gcm_learnt_mobile"
        const val LEARNT_WIFI = "gcm_learnt_wifi"
        const val LEARNT_OTHER = "gcm_learnt_other"

        val PROJECTION = arrayOf(
            FULL_LOG,
            LAST_PERSISTENT_ID,
            CONFIRM_NEW_APPS,
            ENABLE_GCM,
            NETWORK_MOBILE,
            NETWORK_WIFI,
            NETWORK_ROAMING,
            NETWORK_OTHER,
            LEARNT_MOBILE,
            LEARNT_WIFI,
            LEARNT_OTHER,
        )
    }

    object Auth {
        private const val id = "auth"
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, id)
        const val CONTENT_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$id"

        const val TRUST_GOOGLE = "auth_manager_trust_google"
        const val VISIBLE = "auth_manager_visible"

        val PROJECTION = arrayOf(
            TRUST_GOOGLE,
            VISIBLE,
        )
    }

    object Exposure {
        private const val id = "exposureNotification"
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, id)
        const val CONTENT_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$id"

        const val SCANNER_ENABLED = "exposure_scanner_enabled"
        const val LAST_CLEANUP = "exposure_last_cleanup"

        val PROJECTION = arrayOf(
            SCANNER_ENABLED,
            LAST_CLEANUP,
        )
    }

    object SafetyNet {
        private const val id = "safety-net"
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, id)
        const val CONTENT_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$id"
    }

    fun <T> getSettings(context: Context, uri: Uri, projection: Array<out String>?, f: (Cursor) -> T): T {
        context.contentResolver.query(uri, projection, null, null, null).use { c ->
            require(c != null) { "Cursor for query $uri ${projection?.toList()} was null" }
            if (!c.moveToFirst()) error("Cursor for query $uri ${projection?.toList()} was empty")
            return f.invoke(c)
        }
    }

    fun setSettings(context: Context, uri: Uri, v: ContentValues.() -> Unit) {
        val values = ContentValues().apply { v.invoke(this) }
        val affected = context.contentResolver.update(uri, values, null, null)
        require(affected == 1) { "Update for $uri with $values affected 0 rows"}
    }

}
