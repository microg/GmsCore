/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.db.williamchart.data.Scale
import com.db.williamchart.view.BarChartView

class BarChartPreference : Preference {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        layoutResource = R.layout.preference_bar_chart
    }

    private lateinit var chart: BarChartView
    var labelsFormatter: (Float) -> String = { it.toString() }
        set(value) {
            field = value
            if (this::chart.isInitialized) {
                chart.labelsFormatter = value
            }
        }
    var scale: Scale? = null
        set(value) {
            field = value
            if (value != null && this::chart.isInitialized) {
                chart.scale = value
            }
        }
    var data: LinkedHashMap<String, Float> = linkedMapOf()
        set(value) {
            field = value
            if (this::chart.isInitialized) {
                chart.animate(data)
            }
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        chart = holder.itemView as? BarChartView ?: holder.findViewById(R.id.bar_chart) as BarChartView
        chart.labelsFormatter = labelsFormatter
        scale?.let { chart.scale = it }
        chart.animate(data)
    }

}
