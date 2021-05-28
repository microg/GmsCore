/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.google.android.gms.R
import org.microg.gms.checkin.getCheckinServiceInfo
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.McsConstants.ACTION_RECONNECT
import org.microg.gms.gcm.TriggerReceiver
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.gms.safetynet.getSafetyNetServiceInfo
import org.microg.nlp.client.UnifiedLocationClient
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
        findPreference<Preference>(PREF_UNIFIEDNLP)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openUnifiedNlpSettings)
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
        findPreference<Preference>(PREF_ABOUT)!!.summary = getString(R.string.about_version_str, AboutFragment.getSelfVersion(context))
    }

    override fun onResume() {
        super.onResume()
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            updateDetails(appContext)
        }
    }

    private suspend fun updateDetails(context: Context) {
        val gcmServiceInfo = getGcmServiceInfo(context)
        if (gcmServiceInfo.configuration.enabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            // check if we are connected as we should be and re-connect if not
            if (!gcmServiceInfo.connected) {
                context.sendBroadcast(Intent(ACTION_RECONNECT, null, context, TriggerReceiver::class.java))
            }
            database.close()
            findPreference<Preference>(PREF_GCM)!!.summary = getString(R.string.service_status_enabled_short) + " - " + resources.getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount)
        } else {
            findPreference<Preference>(PREF_GCM)!!.setSummary(R.string.service_status_disabled_short)
        }

        findPreference<Preference>(PREF_CHECKIN)!!.setSummary(if (getCheckinServiceInfo(context).configuration.enabled) R.string.service_status_enabled_short else R.string.service_status_disabled_short)
        findPreference<Preference>(PREF_SNET)!!.setSummary(if (getSafetyNetServiceInfo(context).configuration.enabled) R.string.service_status_enabled_short else R.string.service_status_disabled_short)

        val backendCount = UnifiedLocationClient[context].getLocationBackends().size + UnifiedLocationClient[context].getGeocoderBackends().size
        findPreference<Preference>(PREF_UNIFIEDNLP)!!.summary = resources.getQuantityString(R.plurals.pref_unifiednlp_summary, backendCount, backendCount);

        findPreference<Preference>(PREF_EXPOSURE)?.isVisible = NearbyPreferencesIntegration.isAvailable
        findPreference<Preference>(PREF_EXPOSURE)?.icon = NearbyPreferencesIntegration.getIcon(context)
        findPreference<Preference>(PREF_EXPOSURE)?.summary = NearbyPreferencesIntegration.getExposurePreferenceSummary(context)
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_SNET = "pref_snet"
        const val PREF_UNIFIEDNLP = "pref_unifiednlp"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_EXPOSURE = "pref_exposure"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
