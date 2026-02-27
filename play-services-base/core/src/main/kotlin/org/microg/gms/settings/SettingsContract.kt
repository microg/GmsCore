/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.settings

import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.CrossProfileApps
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import androidx.core.net.toUri
import org.microg.gms.crossprofile.CrossProfileRequestActivity
import org.microg.gms.ui.TAG

object SettingsContract {
    const val META_DATA_KEY_SOURCE_PACKAGE = "org.microg.gms.settings:source-package"

    /**
     * Stores keys that are useful only for connecting to the SettingsProvider from
     * main profile in a managed / work profile
     */
    const val CROSS_PROFILE_SHARED_PREFERENCES_NAME = "crossProfile"
    const val CROSS_PROFILE_PERMISSION = "uri"

    fun getAuthority(context: Context): String {
        val metaData = runCatching { context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData }.getOrNull() ?: Bundle.EMPTY
        val sourcePackage = metaData.getString(META_DATA_KEY_SOURCE_PACKAGE, context.packageName)
        return "${sourcePackage}.microg.settings"
    }

    /**
     * URI for preferences local to this profile
     */
    fun getAuthorityUri(context: Context) = "content://${getAuthority(context)}".toUri()

    /* Cross-profile interactivity, granting access to same preferences across all profiles of a user:
     * URI points to our `SettingsProvider` on normal profile and is supposed to point to
     * _primary_ profile's `SettingsProvider` work / managed profile. If this is not yet established,
     * we need to start the `CrossProfileRequestActivity`, which asks `CrossProfileSendActivity` to
     * send it a URI that entitles it to access the primary profile's settings. (This would normally
     * happen while creating the profile from `UserInitReceiver`.)
     */
    fun getCrossProfileSharedAuthorityUri(context: Context): Uri {

        if (SDK_INT < 30) {
            Log.v(TAG, "cross-profile interactivity not possible on this Android version")
            return "content://${getAuthority(context)}".toUri()
        }

        val userManager = context.getSystemService(UserManager::class.java)
        val workProfile = userManager.isManagedProfile

        if (!workProfile) {
            return "content://${getAuthority(context)}".toUri()
        }

        /* Check special shared preferences file if it contains a URI that permits us to access
         * main profile's settings content provider
         */
        val preferences = context.getSharedPreferences(CROSS_PROFILE_SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        if (preferences.contains(CROSS_PROFILE_PERMISSION)) {
            Log.v(TAG, "using work profile stored URI")
            return preferences.getString(CROSS_PROFILE_PERMISSION, null)!!.toUri()
        }

        val crossProfileApps = context.getSystemService(CrossProfileApps::class.java)
        val targetProfiles = crossProfileApps.targetUserProfiles

        if (!crossProfileApps.canInteractAcrossProfiles() || targetProfiles.isEmpty()) {
            Log.w(TAG, "prerequisites for cross-profile interactivity not met: " +
                    "can interact = ${crossProfileApps.canInteractAcrossProfiles()}, " +
                    "#targetProfiles = ${targetProfiles.size}")
            return "content://${getAuthority(context)}".toUri()
        } else {

            Log.d(TAG, "Initiating activity to request storage URI from main profile")
            context.startActivity(Intent(context, CrossProfileRequestActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            })

            // while proper response is not yet available, work on local data :(
            return "content://${getAuthority(context)}".toUri()
        }
    }

    object CheckIn {
        const val ID = "check-in"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

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
        const val ID = "gcm"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

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
        const val ID = "auth"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val TRUST_GOOGLE = "auth_manager_trust_google"
        const val VISIBLE = "auth_manager_visible"
        const val INCLUDE_ANDROID_ID = "auth_include_android_id"
        const val STRIP_DEVICE_NAME = "auth_strip_device_name"
        const val TWO_STEP_VERIFICATION = "auth_two_step_verification"

        val PROJECTION = arrayOf(
            TRUST_GOOGLE,
            VISIBLE,
            INCLUDE_ANDROID_ID,
            STRIP_DEVICE_NAME,
            TWO_STEP_VERIFICATION,
        )
    }

    object Exposure {
        const val ID = "exposureNotification"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val SCANNER_ENABLED = "exposure_scanner_enabled"
        const val LAST_CLEANUP = "exposure_last_cleanup"

        val PROJECTION = arrayOf(
            SCANNER_ENABLED,
            LAST_CLEANUP,
        )
    }

    object SafetyNet {
        const val ID = "safety-net"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val ENABLED = "safetynet_enabled"

        val PROJECTION = arrayOf(
            ENABLED
        )
    }

    object DroidGuard {
        const val ID = "droidguard"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val ENABLED = "droidguard_enabled"
        const val MODE = "droidguard_mode"
        const val NETWORK_SERVER_URL = "droidguard_network_server_url"
        const val FORCE_LOCAL_DISABLED = "droidguard_force_local_disabled"
        const val HARDWARE_ATTESTATION_BLOCKED = "droidguard_block_hw_attestation"

        val PROJECTION = arrayOf(
            ENABLED,
            MODE,
            NETWORK_SERVER_URL,
            FORCE_LOCAL_DISABLED,
            HARDWARE_ATTESTATION_BLOCKED,
        )
    }

    object Profile {
        const val ID = "profile"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val PROFILE = "device_profile"
        const val SERIAL = "device_profile_serial"

        val PROJECTION = arrayOf(
            PROFILE,
            SERIAL
        )
    }

    object Location {
        const val ID = "location"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val WIFI_ICHNAEA = "location_wifi_mls"
        const val WIFI_MOVING = "location_wifi_moving"
        const val WIFI_LEARNING = "location_wifi_learning"
        const val WIFI_CACHING = "location_wifi_caching"
        const val CELL_ICHNAEA = "location_cell_mls"
        const val CELL_LEARNING = "location_cell_learning"
        const val CELL_CACHING = "location_cell_caching"
        const val GEOCODER_NOMINATIM = "location_geocoder_nominatim"
        const val ICHNAEA_ENDPOINT = "location_ichnaea_endpoint"
        const val ONLINE_SOURCE = "location_online_source"
        const val ICHNAEA_CONTRIBUTE = "location_ichnaea_contribute"

        val PROJECTION = arrayOf(
            WIFI_ICHNAEA,
            WIFI_MOVING,
            WIFI_LEARNING,
            WIFI_CACHING,
            CELL_ICHNAEA,
            CELL_LEARNING,
            CELL_CACHING,
            GEOCODER_NOMINATIM,
            ICHNAEA_ENDPOINT,
            ONLINE_SOURCE,
            ICHNAEA_CONTRIBUTE,
        )
    }

    object Vending {
        const val ID = "vending"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val LICENSING = "vending_licensing"
        const val LICENSING_PURCHASE_FREE_APPS = "vending_licensing_purchase_free_apps"
        const val SPLIT_INSTALL = "vending_split_install"
        const val BILLING = "vending_billing"
        const val ASSET_DELIVERY = "vending_asset_delivery"
        const val ASSET_DEVICE_SYNC = "vending_device_sync"
        const val APPS_INSTALL = "vending_apps_install"
        const val APPS_INSTALLER_LIST = "vending_apps_installer_list"
        const val PLAY_INTEGRITY_APP_LIST = "vending_play_integrity_apps"

        val PROJECTION = arrayOf(
            LICENSING,
            LICENSING_PURCHASE_FREE_APPS,
            SPLIT_INSTALL,
            BILLING,
            ASSET_DELIVERY,
            ASSET_DEVICE_SYNC,
            APPS_INSTALL,
            APPS_INSTALLER_LIST,
            PLAY_INTEGRITY_APP_LIST
        )
    }

    object WorkProfile {
        const val ID = "workprofile"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getCrossProfileSharedAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val CREATE_WORK_ACCOUNT = "workprofile_allow_create_work_account"

        val PROJECTION = arrayOf(
            CREATE_WORK_ACCOUNT
        )
    }

    object GameProfile {
        const val ID = "gameprofile"
        fun getContentUri(context: Context) = Uri.withAppendedPath(getCrossProfileSharedAuthorityUri(context), ID)
        fun getContentType(context: Context) = "vnd.android.cursor.item/vnd.${getAuthority(context)}.$ID"

        const val ALLOW_CREATE_PLAYER = "game_allow_create_player"
        const val ALLOW_UPLOAD_GAME_PLAYED = "allow_upload_game_played"

        val PROJECTION = arrayOf(
            ALLOW_CREATE_PLAYER,
            ALLOW_UPLOAD_GAME_PLAYED
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
        val c = context.contentResolver.query(uri, projection, null, null, null)
        try {
            require(c != null) { "Cursor for query $uri ${projection?.toList()} was null" }
            if (!c.moveToFirst()) error("Cursor for query $uri ${projection?.toList()} was empty")
            f.invoke(c)
        } finally {
            c?.close()
        }
    }

    @JvmStatic
    fun setSettings(context: Context, uri: Uri, v: ContentValues.() -> Unit) = withoutCallingIdentity {
        val values = ContentValues().apply { v.invoke(this) }
        val affected = context.contentResolver.update(uri, values, null, null)
        require(affected == 1) { "Update for $uri with $values affected 0 rows"}
    }

}
