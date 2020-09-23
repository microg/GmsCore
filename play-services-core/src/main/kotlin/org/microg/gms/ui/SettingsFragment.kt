/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.cast.media.CastMediaRouteProviderService
import com.mgoogle.android.gms.R
import org.microg.gms.checkin.CheckinClient
import org.microg.gms.checkin.getCheckinServiceInfo
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        findPreference<Preference>(PREF_CHECKIN)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openCheckinSettings)
            true
        }
        findPreference<Preference>(PREF_GCM)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openGcmSettings)
            true
        }
        findPreference<Preference>(PREF_ABOUT)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAbout)
            true
        }
        findPreference<Preference>(PREF_ABOUT)?.summary = getString(R.string.about_version_str, AboutFragment.getSelfVersion(context))

        findPreference<SwitchPreferenceCompat>(PREF_CAST_DOUBLE_FIX_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
            context?.packageManager?.setComponentEnabledSetting(
                    ComponentName(requireActivity().applicationContext, CastMediaRouteProviderService::class.java),
                    when (newValue) {
                        true -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        else -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    },
                    PackageManager.DONT_KILL_APP)
            true
        }

        findPreference<SwitchPreferenceCompat>(BRAND_SPOOF_FIX_ENABLED)!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            CheckinClient.brandSpoof = newValue as Boolean
            true
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            updateDetails()
        }
    }

    private suspend fun updateDetails() {
        findPreference<Preference>(PREF_GCM)?.summary = if (getGcmServiceInfo(requireContext()).configuration.enabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            database.close()
            getString(R.string.service_status_enabled_short) + " - " + resources.getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount)
        } else {
            getString(R.string.service_status_disabled_short)
        }

        findPreference<Preference>(PREF_CHECKIN)?.setSummary(if (getCheckinServiceInfo(requireContext()).configuration.enabled) R.string.service_status_enabled_short else R.string.service_status_disabled_short)
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_CAST_DOUBLE_FIX_ENABLED = "pref_cast_double_fix_enabled"
        const val BRAND_SPOOF_FIX_ENABLED = "brand_spoof_fix_enabled"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
