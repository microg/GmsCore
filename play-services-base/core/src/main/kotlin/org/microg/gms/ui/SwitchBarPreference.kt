/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.PreferenceViewHolder
import androidx.preference.TwoStatePreference
import org.microg.gms.base.core.R

// TODO
class SwitchBarPreference : TwoStatePreference {
    private val frameId: Int
    private val backgroundOn: Drawable?
    private val backgroundOff: Drawable?
    private val backgroundDisabled: Drawable?

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwitchBarPreference, defStyleAttr, defStyleRes)
        frameId = a.getResourceId(R.styleable.SwitchBarPreference_switchBarFrameId, 0)
        backgroundOn = a.getDrawable(R.styleable.SwitchBarPreference_switchBarFrameBackgroundOn)
        backgroundOff = a.getDrawable(R.styleable.SwitchBarPreference_switchBarFrameBackgroundOff)
        backgroundDisabled = a.getDrawable(R.styleable.SwitchBarPreference_switchBarFrameBackgroundDisabled)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, R.style.Preference_SwitchBar)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.switchBarPreferenceStyle)
    constructor(context: Context) : this(context, null)

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
        val frame = if (frameId == 0) null else holder.findViewById(frameId)
        val backgroundView = frame ?: holder.itemView
        val (backgroundDrawable, backgroundColorAttribute) = when {
            !isEnabled -> Pair(backgroundDisabled, androidx.appcompat.R.attr.colorControlHighlight)
            isChecked -> Pair(backgroundOn, androidx.appcompat.R.attr.colorControlActivated)
            else -> Pair(backgroundOff, androidx.appcompat.R.attr.colorButtonNormal)
        }
        if (backgroundDrawable != null) {
            backgroundView.setBackgroundDrawable(backgroundDrawable)
        } else {
            backgroundView.setBackgroundColorAttribute(backgroundColorAttribute)
        }
    }
}