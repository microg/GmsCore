/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.TwoStatePreference
import org.microg.gms.base.core.R

// TODO
class SwitchBarPreference : TwoStatePreference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        layoutResource = R.layout.preference_switch_bar
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedBelow = false
        holder.isDividerAllowedAbove = false
        val switch = holder.findViewById(R.id.switch_widget) as SwitchCompat
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = isChecked
        switch.setOnCheckedChangeListener { view, isChecked ->
            if (!callChangeListener(isChecked)) {
                view.isChecked = !isChecked
                return@setOnCheckedChangeListener
            }
            this.isChecked = isChecked
        }
        holder.itemView.setBackgroundColorAttribute(when {
            isChecked -> androidx.appcompat.R.attr.colorControlActivated
            isEnabled -> androidx.appcompat.R.attr.colorButtonNormal
            else -> androidx.appcompat.R.attr.colorControlHighlight
        })
    }
}

@Deprecated("Get rid")
interface PreferenceSwitchBarCallback {
    fun onChecked(newStatus: Boolean)
}