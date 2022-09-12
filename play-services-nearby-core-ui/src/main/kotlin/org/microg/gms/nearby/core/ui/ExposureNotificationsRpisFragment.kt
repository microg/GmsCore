/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import org.microg.gms.nearby.exposurenotification.ExposureDatabase

@TargetApi(21)
class ExposureNotificationsRpisFragment : PreferenceFragmentCompat() {
    private lateinit var histogramCategory: PreferenceCategory
    private lateinit var histogram: DotChartPreference
    private lateinit var deleteAll: Preference
    private lateinit var exportDb: Preference

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
            // Allow drawing under system navbar / status bar
            fitsSystemWindows = true
            clipToPadding = false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications_rpis)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        histogramCategory = preferenceScreen.findPreference("prefcat_exposure_rpi_histogram") ?: histogramCategory
        histogram = preferenceScreen.findPreference("pref_exposure_rpi_histogram") ?: histogram
        deleteAll = preferenceScreen.findPreference("pref_exposure_rpi_delete_all") ?: deleteAll
        exportDb = preferenceScreen.findPreference("pref_exposure_export_database") ?: exportDb
        deleteAll.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pref_exposure_rpi_delete_all_title)
                    .setView(R.layout.exposure_notifications_confirm_delete)
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
        exportDb.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ExposureDatabase.export(requireContext())
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateChart()
    }

    fun updateChart() {
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            val rpiHourHistogram = ExposureDatabase.with(appContext) { database -> database.rpiHourHistogram }
            val totalRpiCount = rpiHourHistogram.map { it.rpis }.sum()
            deleteAll.isEnabled = totalRpiCount > 0
            histogramCategory.title = getString(R.string.prefcat_exposure_rpis_histogram_title, totalRpiCount)
            histogram.data = rpiHourHistogram
        }
    }
}
