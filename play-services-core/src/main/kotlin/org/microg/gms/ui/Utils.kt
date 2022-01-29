/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import androidx.navigation.navOptions
import androidx.navigation.ui.R

fun PackageManager.getApplicationInfoIfExists(packageName: String?, flags: Int = 0): ApplicationInfo? = packageName?.let {
    try {
        getApplicationInfo(it, flags)
    } catch (e: Exception) {
        Log.w(TAG, "Package $packageName not installed.")
        null
    }
}

fun NavController.navigate(context: Context, @IdRes resId: Int, args: Bundle? = null) {
    navigate(resId, args, if (context.systemAnimationsEnabled) navOptions {
        anim {
            enter = R.anim.nav_default_enter_anim
            exit = R.anim.nav_default_exit_anim
            popEnter = R.anim.nav_default_pop_enter_anim
            popExit = R.anim.nav_default_pop_exit_anim
        }
    } else null)
}

@RequiresApi(Build.VERSION_CODES.M)
fun Context.hideIcon(hide: Boolean) {
    packageManager.setComponentEnabledSetting(
            ComponentName.createRelative(this, "org.microg.gms.ui.SettingsActivityLauncher"),
            when (hide) {
                true -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                false -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            },
            PackageManager.DONT_KILL_APP
    )
}

val Context.systemAnimationsEnabled: Boolean
    get() {
        val duration: Float = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val transition: Float = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)

        return duration != 0f && transition != 0f
    }