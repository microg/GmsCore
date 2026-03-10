/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class AppIconPreference : AppPreference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val icon = holder.findViewById(android.R.id.icon)
        if (icon is ImageView) {
            icon.adjustViewBounds = true
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
            icon.maxHeight = (32.0 * context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT).toInt()
        }
    }
}
