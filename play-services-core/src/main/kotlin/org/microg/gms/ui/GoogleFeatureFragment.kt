/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.R
import org.microg.gms.feature.GoogleFeaturePreferences

class GoogleFeatureFragment : PreferenceFragmentCompat() {
    private lateinit var timelineEnabled: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_feature)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        timelineEnabled = preferenceScreen.findPreference(FEATURE_MANAGER_TIMELINE) ?: timelineEnabled
        timelineEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    GoogleFeaturePreferences.setAllowMapsTimelineFeature(requireContext(), newValue)
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
        lifecycleScope.launchWhenResumed {
            timelineEnabled.isChecked = GoogleFeaturePreferences.allowedMapsTimelineFeature(requireContext())
        }
    }

    companion object {
        const val FEATURE_MANAGER_TIMELINE = "feature_manager_timeline"
    }
}