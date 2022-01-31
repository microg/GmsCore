/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class TextPreference : Preference {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context!!, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context) : super(context)


    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val iconFrame = holder.findViewById(androidx.preference.R.id.icon_frame)
        iconFrame?.layoutParams?.height = MATCH_PARENT
        (iconFrame as? LinearLayout)?.gravity = Gravity.TOP or Gravity.START
        val pad = (context.resources.displayMetrics.densityDpi/160f * 20).toInt()
        iconFrame?.setPadding(0, pad, 0, pad)
        val textView = holder.findViewById(android.R.id.summary) as? TextView
        textView?.maxLines = Int.MAX_VALUE
    }
}
