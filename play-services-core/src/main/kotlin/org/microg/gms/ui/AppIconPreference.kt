/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class AppIconPreference(context: Context) : Preference(context) {
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val icon = holder?.findViewById(android.R.id.icon)
        if (icon is ImageView) {
            icon.adjustViewBounds = true
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
            icon.maxHeight = (32.0 * context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT).toInt()
        }
    }
}
