/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import org.microg.gms.checkin.getCheckinServiceInfo

class DeviceRegistrationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var statusCategory: PreferenceCategory
    private lateinit var status: Preference
    private lateinit var androidId: Preference
    private val handler = Handler()
    private val updateRunnable = Runnable { updateStatus() }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_device_registration)
    }

    override fun onBindPreferences() {
        statusCategory = preferenceScreen.findPreference("prefcat_device_registration_status") ?: statusCategory
        status = preferenceScreen.findPreference("pref_device_registration_status") ?: status
        androidId = preferenceScreen.findPreference("pref_device_registration_android_id") ?: androidId
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateStatus() {
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL)
        lifecycleScope.launchWhenResumed {
            val serviceInfo = getCheckinServiceInfo(requireContext())
            statusCategory.isVisible = serviceInfo.configuration.enabled
            if (serviceInfo.lastCheckin > 0) {
                status.summary = getString(R.string.checkin_last_registration, DateUtils.getRelativeTimeSpanString(serviceInfo.lastCheckin, System.currentTimeMillis(), 0))
                androidId.isVisible = true
                androidId.summary = serviceInfo.androidId.toString(16)
            } else {
                status.summary = getString(R.string.checkin_not_registered)
                androidId.isVisible = false
            }
        }
    }

    companion object {
        private const val UPDATE_INTERVAL = 1000L
    }
}
