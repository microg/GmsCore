/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import org.microg.nlp.app.R
import org.microg.nlp.client.GeocodeClient
import org.microg.nlp.client.LocationClient
import org.microg.nlp.client.UnifiedLocationClient
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        findPreference<Preference>(PREF_UNIFIEDNLP)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openUnifiedNlpSettings)
            true
        }
        findPreference<Preference>(PREF_ABOUT)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAbout)
            true
        }
        findPreference<Preference>(PREF_ABOUT)!!.summary = getString(R.string.about_version_str, AboutFragment.getSelfVersion(context))
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        lifecycleScope.launchWhenResumed {
            updateDetails(context)
        }
    }

    private suspend fun updateDetails(context: Context) {
        val backendCount = try {
            LocationClient(context, lifecycle).getLocationBackends().size + GeocodeClient(context, lifecycle).getGeocodeBackends().size
        } catch (e: Exception) {
            0
        }
        findPreference<Preference>(PREF_UNIFIEDNLP)!!.summary = context.resources.getQuantityString(R.plurals.pref_unifiednlp_summary, backendCount, backendCount)
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_UNIFIEDNLP = "pref_unifiednlp"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
