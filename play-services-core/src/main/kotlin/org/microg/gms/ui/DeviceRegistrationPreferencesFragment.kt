/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import org.microg.gms.checkin.CheckinPrefs
import org.microg.gms.checkin.LastCheckinInfo

class DeviceRegistrationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var statusCategory: PreferenceCategory
    private lateinit var status: Preference
    private val handler = Handler()
    private val updateRunnable = Runnable { updateStatus() }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_device_registration)
    }

    override fun onBindPreferences() {
        statusCategory = preferenceScreen.findPreference("prefcat_device_registration_status") ?: statusCategory
        status = preferenceScreen.findPreference("pref_device_registration_status") ?: status
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
        statusCategory.isVisible = CheckinPrefs.get(context).isEnabled
        val checkinInfo = LastCheckinInfo.read(requireContext())
        status.summary = if (checkinInfo.lastCheckin > 0) {
            getString(R.string.checkin_last_registration, DateUtils.getRelativeTimeSpanString(checkinInfo.lastCheckin, System.currentTimeMillis(), 0))
        } else {
            getString(R.string.checkin_not_registered)
        }
    }

    companion object {
        private const val UPDATE_INTERVAL = 1000L
    }
}
