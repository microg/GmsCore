package org.microg.gms.auth

import android.content.Context
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.Auth

object AuthPrefs {

    @JvmStatic
    fun isTrustGooglePermitted(context: Context): Boolean {
        return SettingsContract.getSettings(context, Auth.getContentUri(context), arrayOf(Auth.TRUST_GOOGLE)) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun isAuthVisible(context: Context): Boolean {
        return SettingsContract.getSettings(context, Auth.getContentUri(context), arrayOf(Auth.VISIBLE)) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun shouldIncludeAndroidId(context: Context): Boolean {
        return SettingsContract.getSettings(context, Auth.getContentUri(context), arrayOf(Auth.INCLUDE_ANDROID_ID)) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun shouldStripDeviceName(context: Context): Boolean {
        return SettingsContract.getSettings(context, Auth.getContentUri(context), arrayOf(Auth.STRIP_DEVICE_NAME)) { c ->
            c.getInt(0) != 0
        }
    }

}
