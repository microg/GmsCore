/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.os.Bundle
import android.os.Handler
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.nearby.exposurenotification.ExposureDatabase
import org.microg.gms.nearby.exposurenotification.getExposureNotificationsServiceInfo
import org.microg.gms.ui.AppIconPreference
import org.microg.gms.ui.getApplicationInfoIfExists
import org.microg.gms.ui.navigate

class ExposureNotificationsPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var exposureEnableInfo: Preference
    private lateinit var exposureApps: PreferenceCategory
    private lateinit var exposureAppsNone: Preference
    private lateinit var collectedRpis: Preference
    private lateinit var advertisingId: Preference
    private val handler = Handler()
    private val updateStatusRunnable = Runnable { updateStatus() }
    private val updateContentRunnable = Runnable { updateContent() }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications)
    }

    override fun onBindPreferences() {
        exposureEnableInfo = preferenceScreen.findPreference("pref_exposure_enable_info") ?: exposureEnableInfo
        exposureApps = preferenceScreen.findPreference("prefcat_exposure_apps") ?: exposureApps
        exposureAppsNone = preferenceScreen.findPreference("pref_exposure_apps_none") ?: exposureAppsNone
        collectedRpis = preferenceScreen.findPreference("pref_exposure_collected_rpis") ?: collectedRpis
        advertisingId = preferenceScreen.findPreference("pref_exposure_advertising_id") ?: advertisingId
        collectedRpis.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openExposureRpis)
            true
        }
    }

    override fun onResume() {
        super.onResume()

        updateStatus()
        updateContent()
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(updateStatusRunnable)
        handler.removeCallbacks(updateContentRunnable)
    }

    private fun updateStatus() {
        lifecycleScope.launchWhenResumed {
            handler.postDelayed(updateStatusRunnable, UPDATE_STATUS_INTERVAL)
            val enabled = getExposureNotificationsServiceInfo(requireContext()).configuration.enabled
            exposureEnableInfo.isVisible = !enabled
            advertisingId.isVisible = enabled
        }
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            handler.postDelayed(updateContentRunnable, UPDATE_CONTENT_INTERVAL)
            val context = requireContext()
            val (apps, lastHourKeys, currentId) = ExposureDatabase.with(context) { database ->
                val apps = database.appList.map { packageName ->
                    context.packageManager.getApplicationInfoIfExists(packageName)
                }.filterNotNull().mapIndexed { idx, applicationInfo ->
                    val pref = AppIconPreference(context)
                    pref.order = idx
                    pref.title = applicationInfo.loadLabel(context.packageManager)
                    pref.icon = applicationInfo.loadIcon(context.packageManager)
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openExposureAppDetails, bundleOf(
                                "package" to applicationInfo.packageName
                        ))
                        true
                    }
                    pref.key = "pref_exposure_app_" + applicationInfo.packageName
                    pref
                }
                val lastHourKeys = database.hourRpiCount
                val currentId = database.currentRpiId
                Triple(apps, lastHourKeys, currentId)
            }
            collectedRpis.summary = getString(R.string.pref_exposure_collected_rpis_summary, lastHourKeys)
            if (currentId != null) {
                advertisingId.isVisible = true
                advertisingId.summary = currentId.toString()
            } else {
                advertisingId.isVisible = false
            }
            exposureApps.removeAll()
            if (apps.isEmpty()) {
                exposureApps.addPreference(exposureAppsNone)
            } else {
                for (app in apps) {
                    exposureApps.addPreference(app)
                }
            }
        }
    }

    companion object {
        private const val UPDATE_STATUS_INTERVAL = 1000L
        private const val UPDATE_CONTENT_INTERVAL = 60000L
    }
}
