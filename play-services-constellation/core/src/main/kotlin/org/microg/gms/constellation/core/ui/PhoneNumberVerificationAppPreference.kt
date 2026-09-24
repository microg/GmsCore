/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.ui

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.microg.gms.constellation.core.R
import org.microg.gms.ui.getApplicationInfoIfExists

class PhoneNumberVerificationAppPreference : Preference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    var packageName: String? = null
        set(value) {
            field = value
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfoIfExists(value)
            title = applicationInfo?.loadLabel(packageManager)?.toString() ?: value
            icon = applicationInfo?.loadIcon(packageManager)
                ?: AppCompatResources.getDrawable(context, android.R.mipmap.sym_def_app_icon)
            notifyChanged()
        }

    var usedAtMillis: Long = 0L
        set(value) {
            field = value
            notifyChanged()
        }

    init {
        isPersistent = false
        layoutResource = R.layout.preference_phone_number_verification_last_use
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val icon = holder.findViewById(android.R.id.icon)
        if (icon is ImageView) {
            icon.adjustViewBounds = true
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
            icon.maxHeight = (32.0 * context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT).toInt()
        }
        holder.findViewById(R.id.phone_number_verification_last_use_time)?.let {
            (it as TextView).text = if (usedAtMillis > 0L) {
                DateUtils.getRelativeTimeSpanString(usedAtMillis)
            } else {
                null
            }
        }
    }
}
