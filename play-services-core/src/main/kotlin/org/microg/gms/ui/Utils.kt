/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.IdRes
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

val Context.systemAnimationsEnabled: Boolean
    get() {
        val duration: Float
        val transition: Float
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            duration = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            transition = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        } else {
            duration = Settings.System.getFloat(contentResolver, Settings.System.ANIMATOR_DURATION_SCALE, 1f)
            transition = Settings.System.getFloat(contentResolver, Settings.System.TRANSITION_ANIMATION_SCALE, 1f)
        }
        return duration != 0f && transition != 0f
    }
