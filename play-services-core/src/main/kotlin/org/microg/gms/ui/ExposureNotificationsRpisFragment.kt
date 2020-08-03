/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.TargetApi
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.db.williamchart.data.Scale
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.nearby.exposurenotification.ExposureDatabase
import java.util.*
import kotlin.math.roundToInt

@TargetApi(21)
class ExposureNotificationsRpisFragment : PreferenceFragmentCompat() {
    private lateinit var histogramCategory: PreferenceCategory
    private lateinit var histogram: BarChartPreference
    private lateinit var database: ExposureDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = ExposureDatabase(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications_rpis)
    }

    override fun onBindPreferences() {
        histogramCategory = preferenceScreen.findPreference("prefcat_exposure_rpi_histogram") ?: histogramCategory
        histogram = preferenceScreen.findPreference("pref_exposure_rpi_histogram") ?: histogram
    }

    override fun onResume() {
        super.onResume()
        updateChart()
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    fun updateChart() {
        lifecycleScope.launchWhenResumed {
            val (totalRpiCount, rpiHistogram) = withContext(Dispatchers.IO) {
                val map = linkedMapOf<String, Float>()
                val lowestDate = Math.round((Date().time / 24 / 60 / 60 / 1000 - 13).toDouble()) * 24 * 60 * 60 * 1000
                for (i in 0..13) {
                    val date = Calendar.getInstance().apply { this.time = Date(lowestDate + i * 24 * 60 * 60 * 1000) }.get(Calendar.DAY_OF_MONTH)
                    map[date.toString()] = 0f
                }
                for (entry in database.rpiHistogram) {
                    val time = Date(entry.key * 24 * 60 * 60 * 1000)
                    val date = Calendar.getInstance().apply { this.time = time }.get(Calendar.DAY_OF_MONTH)
                    map[date.toString()] = entry.value.toFloat()
                }
                val totalRpiCount = database.totalRpiCount
                database.close()
                totalRpiCount to map
            }
            histogramCategory.title = getString(R.string.prefcat_exposure_rpis_histogram_title, totalRpiCount)
            histogram.labelsFormatter = { it.roundToInt().toString() }
            histogram.scale = Scale(0f, rpiHistogram.values.max() ?: 0f)
            histogram.data = rpiHistogram
        }
    }
}
