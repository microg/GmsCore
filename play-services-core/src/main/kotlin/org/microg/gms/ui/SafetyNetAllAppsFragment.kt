/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.safetynet.SafetyNetDatabase
import org.microg.gms.vending.PlayIntegrityData
import org.microg.gms.vending.VendingPreferences

class SafetyNetAllAppsFragment : PreferenceFragmentCompat() {
    private lateinit var database: SafetyNetDatabase
    private lateinit var apps: PreferenceCategory
    private lateinit var appsNone: Preference
    private lateinit var progress: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = SafetyNetDatabase(requireContext())
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_safetynet_all_apps)
        apps = preferenceScreen.findPreference("prefcat_safetynet_apps_all") ?: apps
        appsNone = preferenceScreen.findPreference("pref_safetynet_apps_all_none") ?: appsNone
        progress = preferenceScreen.findPreference("pref_safetynet_apps_all_progress") ?: progress
    }

    private fun updateContent() {
        val context = requireContext()
        lifecycleScope.launchWhenResumed {
            val playIntegrityData = VendingPreferences.getPlayIntegrityAppList(context)
            val apps = withContext(Dispatchers.IO) {
                val playPairs = PlayIntegrityData.loadDataSet(playIntegrityData).map { it.packageName to it.lastTime }
                val res = (database.recentApps + playPairs).map { app ->
                    val pref = AppIconPreference(context)
                    pref.packageName = app.first
                    pref.summary = when {
                        app.second > 0 -> getString(R.string.safetynet_last_run_at, DateUtils.getRelativeTimeSpanString(app.second))
                        else -> null
                    }
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(
                            requireContext(), R.id.openSafetyNetAppDetailsFromAll, bundleOf(
                                "package" to app.first
                            )
                        )
                        true
                    }
                    pref.key = "pref_safetynet_app_" + app.first
                    pref
                }.sortedBy {
                    it.title.toString().lowercase()
                }
                database.close()
                res
            }
            this@SafetyNetAllAppsFragment.apps.removeAll()
            this@SafetyNetAllAppsFragment.apps.isVisible = true

            val appList = if (apps.isEmpty()) listOf(appsNone) else apps

            appList.forEachIndexed { index, pref ->
                pref.layoutResource = chooseLayoutForPosition(index, appList.size)
                this@SafetyNetAllAppsFragment.apps.addPreference(pref)
            }

            appsNone.isVisible = apps.isEmpty()
            progress.isVisible = false
        }
    }

    private fun chooseLayoutForPosition(index: Int, total: Int): Int {
        return when {
            total <= 1 -> R.layout.preference_material_secondary_single
            total == 2 -> if (index == 0) {
                R.layout.preference_material_secondary_top
            } else {
                R.layout.preference_material_secondary_bottom
            }

            else -> when (index) {
                0 -> R.layout.preference_material_secondary_top
                total - 1 -> R.layout.preference_material_secondary_bottom
                else -> R.layout.preference_material_secondary_middle
            }
        }
    }
}
