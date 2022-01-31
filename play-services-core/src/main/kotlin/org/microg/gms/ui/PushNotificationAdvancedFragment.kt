/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mgoogle.android.gms.R
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.gms.gcm.setGcmServiceConfiguration

class PushNotificationAdvancedFragment : PreferenceFragmentCompat() {
    private lateinit var networkMobile: ListPreference
    private lateinit var networkWifi: ListPreference
    private lateinit var networkRoaming: ListPreference
    private lateinit var networkOther: ListPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_gcm_advanced)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        networkMobile = preferenceScreen.findPreference(GcmPrefs.PREF_NETWORK_MOBILE) ?: networkMobile
        networkWifi = preferenceScreen.findPreference(GcmPrefs.PREF_NETWORK_WIFI) ?: networkWifi
        networkRoaming = preferenceScreen.findPreference(GcmPrefs.PREF_NETWORK_ROAMING) ?: networkRoaming
        networkOther = preferenceScreen.findPreference(GcmPrefs.PREF_NETWORK_OTHER) ?: networkOther

        networkMobile.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                (newValue as? String)?.toIntOrNull()?.let {
                    setGcmServiceConfiguration(requireContext(), getGcmServiceInfo(appContext).configuration.copy(mobile = it))
                }
                updateContent()
            }
            true
        }
        networkWifi.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                (newValue as? String)?.toIntOrNull()?.let {
                    setGcmServiceConfiguration(appContext, getGcmServiceInfo(requireContext()).configuration.copy(wifi = it))
                }
                updateContent()
            }
            true
        }
        networkRoaming.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                (newValue as? String)?.toIntOrNull()?.let {
                    setGcmServiceConfiguration(appContext, getGcmServiceInfo(requireContext()).configuration.copy(roaming = it))
                }
                updateContent()
            }
            true
        }
        networkOther.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                (newValue as? String)?.toIntOrNull()?.let {
                    setGcmServiceConfiguration(appContext, getGcmServiceInfo(requireContext()).configuration.copy(other = it))
                }
                updateContent()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    private fun updateContent() {
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            val serviceInfo = getGcmServiceInfo(appContext)
            networkMobile.value = serviceInfo.configuration.mobile.toString()
            networkMobile.summary = getSummaryString(serviceInfo.configuration.mobile, serviceInfo.learntMobileInterval)
            networkWifi.value = serviceInfo.configuration.wifi.toString()
            networkWifi.summary = getSummaryString(serviceInfo.configuration.wifi, serviceInfo.learntWifiInterval)
            networkRoaming.value = serviceInfo.configuration.roaming.toString()
            networkRoaming.summary = getSummaryString(serviceInfo.configuration.roaming, serviceInfo.learntMobileInterval)
            networkOther.value = serviceInfo.configuration.other.toString()
            networkOther.summary = getSummaryString(serviceInfo.configuration.other, serviceInfo.learntOtherInterval)
        }
    }

    private fun getSummaryString(value: Int, learnt: Int): String = when (value) {
        -1 -> getString(R.string.service_status_disabled_short)
        0 -> getString(R.string.service_status_enabled_short) + " / " + getString(R.string.gcm_status_pref_default) + ": " + getHeartbeatString(learnt)
        else -> getString(R.string.service_status_enabled_short) + " / " + getString(R.string.gcm_status_pref_manual) + ": " + getHeartbeatString(value * GcmPrefs.INTERVAL)
    }

    private fun getHeartbeatString(heartbeatMs: Int): String {
        return if (heartbeatMs < 120000) {
            (heartbeatMs / 1000).toString() + " " + getString(R.string.gcm_status_pref_sec)
        } else (heartbeatMs / GcmPrefs.INTERVAL).toString() + " " + getString(R.string.gcm_status_pref_min)
    }

    companion object {
        private val HEARTBEAT_PREFS = arrayOf(GcmPrefs.PREF_NETWORK_MOBILE, GcmPrefs.PREF_NETWORK_ROAMING, GcmPrefs.PREF_NETWORK_WIFI, GcmPrefs.PREF_NETWORK_OTHER)
    }
}