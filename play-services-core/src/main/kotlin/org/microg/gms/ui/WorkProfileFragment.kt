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
import org.microg.gms.vending.VendingPreferences
import org.microg.gms.workprofile.WorkProfilePreferences

class WorkProfileFragment : PreferenceFragmentCompat() {
    private lateinit var workProfileEnabled: SwitchBarPreference

    private lateinit var workProfilePreferences: WorkProfilePreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_work_profile)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {

        workProfilePreferences = WorkProfilePreferences(requireContext().applicationContext)

        workProfileEnabled = preferenceScreen.findPreference(PREF_CREATE_ACCOUNT) ?: workProfileEnabled
        workProfileEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    workProfilePreferences.allowCreateWorkAccount = newValue
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
            workProfileEnabled.isChecked = workProfilePreferences.allowCreateWorkAccount
        }
    }

    companion object {
        const val PREF_CREATE_ACCOUNT = "workprofile_allow_create_work_account"
    }
}