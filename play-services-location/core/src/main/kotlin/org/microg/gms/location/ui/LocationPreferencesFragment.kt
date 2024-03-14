/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.ui

import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.location.LocationSettings
import org.microg.gms.location.core.R
import org.microg.gms.location.hasMozillaLocationServiceSupport
import org.microg.gms.location.hasNetworkLocationServiceBuiltIn
import org.microg.gms.location.manager.LocationAppsDatabase
import org.microg.gms.ui.AppIconPreference
import org.microg.gms.ui.getApplicationInfoIfExists
import org.microg.gms.ui.navigate

class LocationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var locationApps: PreferenceCategory
    private lateinit var locationAppsAll: Preference
    private lateinit var locationAppsNone: Preference
    private lateinit var networkProviderCategory: PreferenceCategory
    private lateinit var wifiMls: TwoStatePreference
    private lateinit var wifiMoving: TwoStatePreference
    private lateinit var wifiLearning: TwoStatePreference
    private lateinit var cellMls: TwoStatePreference
    private lateinit var cellLearning: TwoStatePreference
    private lateinit var nominatim: TwoStatePreference
    private lateinit var database: LocationAppsDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LocationAppsDatabase(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_location)

        locationApps = preferenceScreen.findPreference("prefcat_location_apps") ?: locationApps
        locationAppsAll = preferenceScreen.findPreference("pref_location_apps_all") ?: locationAppsAll
        locationAppsNone = preferenceScreen.findPreference("pref_location_apps_none") ?: locationAppsNone
        networkProviderCategory = preferenceScreen.findPreference("prefcat_location_network_provider") ?: networkProviderCategory
        wifiMls = preferenceScreen.findPreference("pref_location_wifi_mls_enabled") ?: wifiMls
        wifiMoving = preferenceScreen.findPreference("pref_location_wifi_moving_enabled") ?: wifiMoving
        wifiLearning = preferenceScreen.findPreference("pref_location_wifi_learning_enabled") ?: wifiLearning
        cellMls = preferenceScreen.findPreference("pref_location_cell_mls_enabled") ?: cellMls
        cellLearning = preferenceScreen.findPreference("pref_location_cell_learning_enabled") ?: cellLearning
        nominatim = preferenceScreen.findPreference("pref_geocoder_nominatim_enabled") ?: nominatim

        locationAppsAll.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAllLocationApps)
            true
        }
        wifiMls.setOnPreferenceChangeListener { _, newValue ->
            LocationSettings(requireContext()).wifiMls = newValue as Boolean
            true
        }
        wifiMoving.setOnPreferenceChangeListener { _, newValue ->
            LocationSettings(requireContext()).wifiMoving = newValue as Boolean
            true
        }
        wifiLearning.setOnPreferenceChangeListener { _, newValue ->
            LocationSettings(requireContext()).wifiLearning = newValue as Boolean
            true
        }
        cellMls.setOnPreferenceChangeListener { _, newValue ->
            LocationSettings(requireContext()).cellMls = newValue as Boolean
            true
        }
        cellLearning.setOnPreferenceChangeListener { _, newValue ->
            LocationSettings(requireContext()).cellLearning = newValue as Boolean
            true
        }
        nominatim.setOnPreferenceChangeListener { _, newValue ->
            LocationSettings(requireContext()).geocoderNominatim = newValue as Boolean
            true
        }

        networkProviderCategory.isVisible = requireContext().hasNetworkLocationServiceBuiltIn()
        wifiMls.isVisible = requireContext().hasMozillaLocationServiceSupport()
        cellMls.isVisible = requireContext().hasMozillaLocationServiceSupport()
        wifiLearning.isVisible =
            SDK_INT >= 17 && requireContext().getSystemService<LocationManager>()?.allProviders.orEmpty().contains(LocationManager.GPS_PROVIDER)
        cellLearning.isVisible =
            SDK_INT >= 17 && requireContext().getSystemService<LocationManager>()?.allProviders.orEmpty().contains(LocationManager.GPS_PROVIDER)
    }

    override fun onResume() {
        super.onResume()
        runCatching { updateContent() }.onFailure { database.close() }
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            wifiMls.isChecked = LocationSettings(context).wifiMls
            wifiMoving.isChecked = LocationSettings(context).wifiMoving
            wifiLearning.isChecked = LocationSettings(context).wifiLearning
            cellMls.isChecked = LocationSettings(context).cellMls
            cellLearning.isChecked = LocationSettings(context).cellLearning
            nominatim.isChecked = LocationSettings(context).geocoderNominatim
            val (apps, showAll) = withContext(Dispatchers.IO) {
                val apps = database.listAppsByAccessTime()
                val res = apps.map { app ->
                    app to context.packageManager.getApplicationInfoIfExists(app.first)
                }.mapNotNull { (app, info) ->
                    if (info == null) null else app to info
                }.take(3).mapIndexed { idx, (app, applicationInfo) ->
                    val pref = AppIconPreference(context)
                    pref.order = idx
                    pref.applicationInfo = applicationInfo
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openLocationAppDetails, bundleOf("package" to app.first))
                        true
                    }
                    pref.key = "pref_location_app_" + app.first
                    pref
                }.let { it to (it.size < apps.size) }
                database.close()
                res
            }
            locationAppsAll.isVisible = showAll
            locationApps.removeAll()
            for (app in apps) {
                locationApps.addPreference(app)
            }
            if (showAll) {
                locationApps.addPreference(locationAppsAll)
            } else if (apps.isEmpty()) {
                locationApps.addPreference(locationAppsNone)
            }
        }
    }
}