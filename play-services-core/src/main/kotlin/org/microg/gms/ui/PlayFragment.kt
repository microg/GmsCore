/*
 * SPDX-FileCopyrightText: 2023, e Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.gms.R
import org.microg.gms.play.PlayPreferences

class PlayFragment : PreferenceFragmentCompat() {
    private lateinit var licensingEnabled: TwoStatePreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_play)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        licensingEnabled = preferenceScreen.findPreference(PREF_LICENSING_ENABLED) ?: licensingEnabled
        licensingEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    PlayPreferences.setLicensingEnabled(appContext, newValue)
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
            licensingEnabled.isChecked = PlayPreferences.isLicensingEnabled(appContext)
        }
    }

    companion object {
        const val PREF_LICENSING_ENABLED = "play_licensing"
    }
}