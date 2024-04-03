/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.navigation.NavController
import androidx.navigation.navOptions
import androidx.navigation.ui.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
        if (SDK_INT >= 17) {
            duration = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            transition = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        } else {
            duration = Settings.System.getFloat(contentResolver, Settings.System.ANIMATOR_DURATION_SCALE, 1f)
            transition = Settings.System.getFloat(contentResolver, Settings.System.TRANSITION_ANIMATION_SCALE, 1f)
        }
        return duration != 0f && transition != 0f
    }

fun Context.buildAlertDialog() = try {
    // Try material design first
    MaterialAlertDialogBuilder(this)
} catch (e: Exception) {
    AlertDialog.Builder(this)
}

@ColorInt
fun Context.resolveColor(@AttrRes resid: Int): Int? {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(resid, typedValue, true)) return null
    val colorRes = if (typedValue.resourceId != 0) typedValue.resourceId else typedValue.data
    return ContextCompat.getColor(this, colorRes)
}

@BindingAdapter("app:backgroundColorAttr")
fun View.setBackgroundColorAttribute(@AttrRes resId: Int) = context.resolveColor(resId)?.let { setBackgroundColor(it) }
