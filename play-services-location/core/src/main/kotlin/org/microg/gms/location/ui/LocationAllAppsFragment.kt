/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.ui

import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.location.manager.LocationAppsDatabase
import org.microg.gms.ui.AppIconPreference
import org.microg.gms.ui.getApplicationInfoIfExists
import org.microg.gms.ui.navigate
import org.microg.gms.location.core.R

class LocationAllAppsFragment : PreferenceFragmentCompat() {
    private lateinit var progress: Preference
    private lateinit var locationApps: PreferenceCategory
    private lateinit var database: LocationAppsDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LocationAppsDatabase(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_location_all_apps)
        progress = preferenceScreen.findPreference("pref_location_apps_all_progress") ?: progress
        locationApps = preferenceScreen.findPreference("prefcat_location_apps") ?: locationApps
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }


    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            val apps = withContext(Dispatchers.IO) {
                val res = database.listAppsByAccessTime().map { app ->
                    app to context.packageManager.getApplicationInfoIfExists(app.first)
                }.map { (app, applicationInfo) ->
                    val pref = AppIconPreference(context)
                    pref.title = applicationInfo?.loadLabel(context.packageManager) ?: app.first
                    pref.summary = getString(R.string.location_app_last_access_at, DateUtils.getRelativeTimeSpanString(app.second))
                    pref.icon = applicationInfo?.loadIcon(context.packageManager) ?: AppCompatResources.getDrawable(context, android.R.mipmap.sym_def_app_icon)
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openLocationAppDetailsFromAll, bundleOf("package" to app.first))
                        true
                    }
                    pref.key = "pref_location_app_" + app.first
                    pref
                }.sortedBy {
                    it.title.toString().toLowerCase()
                }.mapIndexed { idx, pair ->
                    pair.order = idx
                    pair
                }
                database.close()
                res
            }
            locationApps.removeAll()
            locationApps.isVisible = true
            for (app in apps) {
                locationApps.addPreference(app)
            }
            progress.isVisible = false
        }
    }
}