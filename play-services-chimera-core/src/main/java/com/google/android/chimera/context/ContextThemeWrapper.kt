/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.context

import android.R
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.view.LayoutInflater
import com.google.android.chimera.annotation.ChimeraApiVersion

@ChimeraApiVersion(added = 0)
open class ContextThemeWrapper : ContextWrapper {
    private var themeResId: Int = 0
    private var currentTheme: Resources.Theme? = null
    private var cachedInflater: LayoutInflater? = null

    protected constructor(base: Context?) : super(base)

    constructor(base: Context?, themeResId: Int) : super(base) {
        this.themeResId = themeResId
    }

    constructor(base: Context?, theme: Resources.Theme?) : super(base) {
        this.currentTheme = theme
    }

    private fun ensureTheme() {
        val firstInit = currentTheme == null
        if (firstInit) {
            currentTheme = resources.newTheme()
            baseContext.theme?.let { baseTheme ->
                currentTheme?.setTo(baseTheme)
            }
        }
        onApplyThemeResource(currentTheme!!, themeResId, firstInit)
    }

    override fun getAssets(): AssetManager {
        return resources.assets
    }

    override fun getSystemService(name: String): Any? {
        return if (LAYOUT_INFLATER_SERVICE != name) {
            super.getSystemService(name)
        } else {
            if (cachedInflater == null) {
                cachedInflater = LayoutInflater.from(baseContext).cloneInContext(this)
            }
            cachedInflater
        }
    }

    override fun getTheme(): Resources.Theme {
        if (currentTheme != null) {
            return currentTheme!!
        }
        var resolvedThemeResId = themeResId
        val targetSdk = applicationInfo.targetSdkVersion
        if (resolvedThemeResId == 0) {
            resolvedThemeResId = when {
                targetSdk < 11 -> R.style.Theme
                targetSdk < 14 -> R.style.Theme_Holo
                targetSdk < 24 -> R.style.Theme_DeviceDefault
                else -> R.style.Theme_DeviceDefault_Light_DarkActionBar
            }
        }
        themeResId = resolvedThemeResId
        ensureTheme()
        return currentTheme!!
    }

    protected open fun onApplyThemeResource(theme: Resources.Theme, resId: Int, first: Boolean) {
        theme.applyStyle(resId, true)
    }

    override fun setTheme(resId: Int) {
        if (themeResId != resId) {
            themeResId = resId
            ensureTheme()
        }
    }
}
