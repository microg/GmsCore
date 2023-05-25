/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference

abstract class AppPreference : Preference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        isPersistent = false
    }

    private var packageNameField: String? = null

    var applicationInfo: ApplicationInfo?
        get() = context.packageManager.getApplicationInfoIfExists(packageNameField)
        set(value) {
            if (value == null && packageNameField != null) {
                title = null
                icon = null
            } else if (value != null) {
                val pm = context.packageManager
                title = value.loadLabel(pm) ?: value.packageName
                icon = value.loadIcon(pm) ?: AppCompatResources.getDrawable(context, android.R.mipmap.sym_def_app_icon)
            }
            packageNameField = value?.packageName
        }

    var packageName: String?
        get() = packageNameField
        set(value) {
            if (value == null && packageNameField != null) {
                title = null
                icon = null
            } else if (value != null) {
                val pm = context.packageManager
                val applicationInfo = pm.getApplicationInfoIfExists(value)
                title = applicationInfo?.loadLabel(pm)?.toString() ?: value
                icon = applicationInfo?.loadIcon(pm) ?: AppCompatResources.getDrawable(context, android.R.mipmap.sym_def_app_icon)
            }
            packageNameField = value
        }
}