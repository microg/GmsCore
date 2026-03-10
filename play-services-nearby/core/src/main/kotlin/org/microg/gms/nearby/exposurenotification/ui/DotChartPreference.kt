/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.microg.gms.nearby.core.R
import org.microg.gms.nearby.exposurenotification.ExposureScanSummary

class DotChartPreference : Preference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        layoutResource = R.layout.preference_dot_chart
    }

    private lateinit var chart: DotChartView
    var data: Set<ExposureScanSummary> = emptySet()
        set(value) {
            field = value
            if (this::chart.isInitialized) {
                chart.data = data
            }
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        chart = holder.itemView as? DotChartView ?: holder.findViewById(R.id.dot_chart) as DotChartView
        chart.data = data
    }

}
