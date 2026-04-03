/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.gms.R
import org.microg.gms.wearable.WearablePreferences

class WearableFragment : PreferenceFragmentCompat() {
    private lateinit var autoAcceptTos: TwoStatePreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_wearable)

        autoAcceptTos = preferenceScreen.findPreference(PREF_AUTO_ACCEPT_TOS)!!
        autoAcceptTos.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    WearablePreferences.setAutoAcceptTosEnabled(appContext, newValue)
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
            autoAcceptTos.isChecked = WearablePreferences.isAutoAcceptTosEnabled(appContext)
        }
    }

    companion object {
        const val PREF_AUTO_ACCEPT_TOS = "wearable_auto_accept_tos"
    }
}
