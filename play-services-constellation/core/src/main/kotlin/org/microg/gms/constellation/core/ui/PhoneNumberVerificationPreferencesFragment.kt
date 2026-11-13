/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.ui

import android.os.Bundle
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.microg.gms.constellation.core.ConstellationStateStore
import org.microg.gms.constellation.core.R
import org.microg.gms.ui.SwitchBarPreference

class PhoneNumberVerificationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var enabled: SwitchBarPreference
    private lateinit var lastUseCategory: PreferenceCategory
    private lateinit var app: PhoneNumberVerificationAppPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_phone_number_verification)
    }

    override fun onBindPreferences() {
        enabled = preferenceScreen.findPreference(PREF_ENABLED) ?: enabled
        lastUseCategory = preferenceScreen.findPreference(PREF_LAST_USE_CATEGORY) ?: lastUseCategory
        app = preferenceScreen.findPreference(PREF_LAST_USE) ?: app
        enabled.setOnPreferenceChangeListener { _, newValue ->
            ConstellationStateStore.setPhoneNumberVerificationEnabled(
                requireContext(),
                newValue as Boolean
            )
            true
        }
    }

    override fun onResume() {
        super.onResume()
        enabled.isChecked =
            ConstellationStateStore.isPhoneNumberVerificationEnabled(requireContext())
        val record = ConstellationStateStore.loadLastPhoneNumberVerification(requireContext())
        lastUseCategory.isVisible = record != null
        if (record != null) {
            app.packageName = record.packageName
            app.summary = getString(
                if (record.successful) {
                    R.string.phone_number_verification_result_success
                } else {
                    R.string.phone_number_verification_result_failure
                }
            )
            app.usedAtMillis = record.usedAtMillis
        }
    }

    companion object {
        private const val PREF_ENABLED = "pref_phone_number_verification_enabled"
        private const val PREF_LAST_USE_CATEGORY = "prefcat_phone_number_verification_last_use"
        private const val PREF_LAST_USE = "pref_phone_number_verification_last_use"
    }
}
