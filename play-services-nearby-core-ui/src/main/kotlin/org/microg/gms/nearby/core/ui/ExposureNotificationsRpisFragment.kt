/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.annotation.TargetApi
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.db.williamchart.data.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.nearby.exposurenotification.ExposureDatabase
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@TargetApi(21)
class ExposureNotificationsRpisFragment : PreferenceFragmentCompat() {
    private lateinit var histogramCategory: PreferenceCategory
    private lateinit var histogram: BarChartPreference
    private lateinit var deleteAll: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications_rpis)
    }

    override fun onBindPreferences() {
        histogramCategory = preferenceScreen.findPreference("prefcat_exposure_rpi_histogram") ?: histogramCategory
        histogram = preferenceScreen.findPreference("pref_exposure_rpi_histogram") ?: histogram
        deleteAll = preferenceScreen.findPreference("pref_exposure_rpi_delete_all") ?: deleteAll
        deleteAll.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pref_exposure_rpi_delete_all_title)
                    .setView(R.layout.exposure_notification_confirm_delete)
                    .setPositiveButton(R.string.pref_exposure_rpi_delete_all_warning_confirm_button) { _, _ ->
                        lifecycleScope.launchWhenStarted {
                            ExposureDatabase.with(requireContext()) { it.deleteAllCollectedAdvertisements() }
                            updateChart()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .create()
                    .show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateChart()
    }

    fun updateChart() {
        lifecycleScope.launchWhenResumed {
            val (totalRpiCount, rpiHistogram) = ExposureDatabase.with(requireContext()) { database ->
                val map = linkedMapOf<String, Float>()
                val lowestDate = (System.currentTimeMillis() / 24 / 60 / 60 / 1000 - 13).toDouble().roundToLong() * 24 * 60 * 60 * 1000
                for (i in 0..13) {
                    val date = Calendar.getInstance().apply { this.time = Date(lowestDate + i * 24 * 60 * 60 * 1000) }.get(Calendar.DAY_OF_MONTH)
                    val str = when (i) {
                        0, 13 -> DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"), lowestDate + i * 24 * 60 * 60 * 1000).toString()
                        else -> IntArray(date).joinToString("").replace("0", "\u200B")
                    }
                    map[str] = 0f
                }
                val refDateLow = Calendar.getInstance().apply { this.time = Date(lowestDate) }.get(Calendar.DAY_OF_MONTH)
                val refDateHigh = Calendar.getInstance().apply { this.time = Date(lowestDate + 13 * 24 * 60 * 60 * 1000) }.get(Calendar.DAY_OF_MONTH)
                for (entry in database.rpiHistogram) {
                    val time = Date(entry.key * 24 * 60 * 60 * 1000)
                    if (time.time < lowestDate) continue // Ignore old data
                    val date = Calendar.getInstance().apply { this.time = time }.get(Calendar.DAY_OF_MONTH)
                    val str = when (date) {
                        refDateLow, refDateHigh -> DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"), entry.key * 24 * 60 * 60 * 1000).toString()
                        else -> IntArray(date).joinToString("").replace("0", "\u200B")
                    }
                    map[str] = entry.value.toFloat()
                }
                val totalRpiCount = database.totalRpiCount
                totalRpiCount to map
            }
            deleteAll.isEnabled = totalRpiCount != 0L
            histogramCategory.title = getString(R.string.prefcat_exposure_rpis_histogram_title, totalRpiCount)
            histogram.labelsFormatter = { it.roundToInt().toString() }
            histogram.scale = Scale(0f, rpiHistogram.values.max()?.coerceAtLeast(0.1f) ?: 0.1f)
            histogram.data = rpiHistogram
        }
    }
}
