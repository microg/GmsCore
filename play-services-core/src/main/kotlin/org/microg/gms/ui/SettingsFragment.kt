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
import com.google.android.gms.R
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.gms.safetynet.SafetyNetPreferences
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        findPreference<Preference>(PREF_CHECKIN)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openCheckinSettings)
            true
        }
        findPreference<Preference>(PREF_GCM)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openGcmSettings)
            true
        }
        findPreference<Preference>(PREF_SNET)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openSafetyNetSettings)
            true
        }
        findPreference<Preference>(PREF_LOCATION)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openLocationSettings)
            true
        }
        findPreference<Preference>(PREF_EXPOSURE)?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), NearbyPreferencesIntegration.exposureNotificationNavigationId)
            true
        }
        findPreference<Preference>(PREF_ABOUT)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAbout)
            true
        }
        findPreference<Preference>(PREF_ABOUT)!!.summary = getString(org.microg.tools.ui.R.string.about_version_str, AboutFragment.getSelfVersion(context))
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        if (GcmPrefs.get(requireContext()).isEnabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            database.close()
            findPreference<Preference>(PREF_GCM)!!.summary = context.getString(R.string.service_status_enabled_short) + " - " + context.resources.getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount)
        } else {
            findPreference<Preference>(PREF_GCM)!!.setSummary(R.string.service_status_disabled_short)
        }

        findPreference<Preference>(PREF_CHECKIN)!!.setSummary(if (CheckinPreferences.isEnabled(requireContext())) R.string.service_status_enabled_short else R.string.service_status_disabled_short)
        findPreference<Preference>(PREF_SNET)!!.setSummary(if (SafetyNetPreferences.isEnabled(requireContext())) R.string.service_status_enabled_short else R.string.service_status_disabled_short)

        findPreference<Preference>(PREF_EXPOSURE)?.isVisible = NearbyPreferencesIntegration.isAvailable
        findPreference<Preference>(PREF_EXPOSURE)?.icon = NearbyPreferencesIntegration.getIcon(requireContext())
        findPreference<Preference>(PREF_EXPOSURE)?.summary = NearbyPreferencesIntegration.getExposurePreferenceSummary(context)
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_SNET = "pref_snet"
        const val PREF_LOCATION = "pref_location"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_EXPOSURE = "pref_exposure"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
