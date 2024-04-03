/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.ui

import android.content.DialogInterface
import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.location.LocationSettings
import org.microg.gms.location.core.R
import org.microg.gms.location.hasIchnaeaLocationServiceSupport
import org.microg.gms.location.hasNetworkLocationServiceBuiltIn
import org.microg.gms.location.manager.LocationAppsDatabase
import org.microg.gms.ui.AppIconPreference
import org.microg.gms.ui.buildAlertDialog
import org.microg.gms.ui.getApplicationInfoIfExists
import org.microg.gms.ui.navigate

class LocationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var locationApps: PreferenceCategory
    private lateinit var locationAppsAll: Preference
    private lateinit var locationAppsNone: Preference
    private lateinit var networkProviderCategory: PreferenceCategory
    private lateinit var wifiIchnaea: TwoStatePreference
    private lateinit var wifiMoving: TwoStatePreference
    private lateinit var wifiLearning: TwoStatePreference
    private lateinit var cellIchnaea: TwoStatePreference
    private lateinit var cellLearning: TwoStatePreference
    private lateinit var nominatim: TwoStatePreference
    private lateinit var database: LocationAppsDatabase

    init {
        setHasOptionsMenu(true)
    }

    companion object {
        private const val MENU_ICHNAEA_URL = Menu.FIRST
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (requireContext().hasIchnaeaLocationServiceSupport()) {
            menu.add(0, MENU_ICHNAEA_URL, 0, R.string.pref_location_custom_url_title)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_ICHNAEA_URL) {
            val view = layoutInflater.inflate(R.layout.preference_location_custom_url, null)
            view.findViewById<EditText>(android.R.id.edit).setText(LocationSettings(requireContext()).ichneaeEndpoint)
            requireContext().buildAlertDialog()
                .setTitle(R.string.pref_location_custom_url_title)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    LocationSettings(requireContext()).ichneaeEndpoint = view.findViewById<EditText>(android.R.id.edit).text.toString()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setNeutralButton(R.string.pref_location_custom_url_reset) { _, _ -> LocationSettings(requireContext()).ichneaeEndpoint = "" }
                .setView(view)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

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
        wifiIchnaea = preferenceScreen.findPreference("pref_location_wifi_mls_enabled") ?: wifiIchnaea
        wifiMoving = preferenceScreen.findPreference("pref_location_wifi_moving_enabled") ?: wifiMoving
        wifiLearning = preferenceScreen.findPreference("pref_location_wifi_learning_enabled") ?: wifiLearning
        cellIchnaea = preferenceScreen.findPreference("pref_location_cell_mls_enabled") ?: cellIchnaea
        cellLearning = preferenceScreen.findPreference("pref_location_cell_learning_enabled") ?: cellLearning
        nominatim = preferenceScreen.findPreference("pref_geocoder_nominatim_enabled") ?: nominatim

        locationAppsAll.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAllLocationApps)
            true
        }
        fun configureChangeListener(preference: TwoStatePreference, listener: (Boolean) -> Unit) {
            preference.setOnPreferenceChangeListener { _, newValue ->
                listener(newValue as Boolean)
                true
            }
        }
        configureChangeListener(wifiIchnaea) { LocationSettings(requireContext()).wifiIchnaea = it }
        configureChangeListener(wifiMoving) { LocationSettings(requireContext()).wifiMoving = it }
        configureChangeListener(wifiLearning) { LocationSettings(requireContext()).wifiLearning = it }
        configureChangeListener(cellIchnaea) { LocationSettings(requireContext()).cellIchnaea = it }
        configureChangeListener(cellLearning) { LocationSettings(requireContext()).cellLearning = it }
        configureChangeListener(nominatim) { LocationSettings(requireContext()).geocoderNominatim = it }

        networkProviderCategory.isVisible = requireContext().hasNetworkLocationServiceBuiltIn()
        wifiIchnaea.isVisible = requireContext().hasIchnaeaLocationServiceSupport()
        cellIchnaea.isVisible = requireContext().hasIchnaeaLocationServiceSupport()
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
            wifiIchnaea.isChecked = LocationSettings(context).wifiIchnaea
            wifiMoving.isChecked = LocationSettings(context).wifiMoving
            wifiLearning.isChecked = LocationSettings(context).wifiLearning
            cellIchnaea.isChecked = LocationSettings(context).cellIchnaea
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