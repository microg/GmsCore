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

}
