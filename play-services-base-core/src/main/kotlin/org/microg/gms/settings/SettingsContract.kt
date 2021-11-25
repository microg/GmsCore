/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.settings

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder

object SettingsContract {
    fun getAuthority(context: Context) = "${context.packageName}.microg.settings"
    fun getAuthorityUri(context: Context): Uri = Uri.parse("content://${getAuthority(context)}")

    object CheckIn {
        private const val id = "check-in"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), id)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$id"

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
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), id)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$id"

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
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), id)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$id"

        const val TRUST_GOOGLE = "auth_manager_trust_google"
        const val VISIBLE = "auth_manager_visible"

        val PROJECTION = arrayOf(
            TRUST_GOOGLE,
            VISIBLE,
        )
    }

    object Exposure {
        private const val id = "exposureNotification"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), id)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$id"

        const val SCANNER_ENABLED = "exposure_scanner_enabled"
        const val LAST_CLEANUP = "exposure_last_cleanup"

        val PROJECTION = arrayOf(
            SCANNER_ENABLED,
            LAST_CLEANUP,
        )
    }

    object SafetyNet {
        private const val id = "safety-net"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), id)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$id"

        const val ENABLED = "safetynet_enabled"

        val PROJECTION = arrayOf(
            ENABLED
        )
    }

    object DroidGuard {
        private const val id = "droidguard"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), id)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$id"

        const val ENABLED = "droidguard_enabled"
        const val MODE = "droidguard_mode"
        const val NETWORK_SERVER_URL = "droidguard_network_server_url"

        val PROJECTION = arrayOf(
            ENABLED,
            MODE,
            NETWORK_SERVER_URL
        )
    }

    object Profile {
        private const val id = "profile"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), id)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$id"

        const val PROFILE = "device_profile"
        const val SERIAL = "device_profile_serial"

        val PROJECTION = arrayOf(
            PROFILE,
            SERIAL
        )
    }

    private fun <T> withoutCallingIdentity(f: () -> T): T {
        val identity = Binder.clearCallingIdentity()
        try {
            return f.invoke()
        } finally {
            Binder.restoreCallingIdentity(identity)
        }
    }

    @JvmStatic
    fun <T> getSettings(context: Context, uri: Uri, projection: Array<out String>?, f: (Cursor) -> T): T = withoutCallingIdentity {
        context.contentResolver.query(uri, projection, null, null, null).use { c ->
            require(c != null) { "Cursor for query $uri ${projection?.toList()} was null" }
            if (!c.moveToFirst()) error("Cursor for query $uri ${projection?.toList()} was empty")
            f.invoke(c)
        }
    }

    @JvmStatic
    fun setSettings(context: Context, uri: Uri, v: ContentValues.() -> Unit) = withoutCallingIdentity {
        val values = ContentValues().apply { v.invoke(this) }
        val affected = context.contentResolver.update(uri, values, null, null)
        require(affected == 1) { "Update for $uri with $values affected 0 rows"}
    }

}
