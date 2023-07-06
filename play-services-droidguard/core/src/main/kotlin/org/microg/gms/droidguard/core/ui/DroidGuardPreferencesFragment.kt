/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.microg.gms.droidguard.core.DroidGuardPreferences
import org.microg.gms.droidguard.core.DroidGuardPreferences.Mode.Embedded
import org.microg.gms.droidguard.core.DroidGuardPreferences.Mode.Network
import org.microg.gms.droidguard.core.R
import org.microg.gms.droidguard.core.R.drawable.ic_radio_checked
import org.microg.gms.droidguard.core.R.drawable.ic_radio_unchecked

class DroidGuardPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var modeEmbedded: Preference
    private lateinit var modeNetwork: ContainedEditTextPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_droidguard)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        modeEmbedded = preferenceScreen.findPreference("pref_droidguard_mode_embedded") ?: modeEmbedded
        modeNetwork = preferenceScreen.findPreference("pref_droidguard_mode_network") ?: modeNetwork
        modeEmbedded.setOnPreferenceClickListener {
            DroidGuardPreferences.setMode(it.context, Embedded)
            updateConfiguration()
            true
        }
        modeNetwork.setOnPreferenceClickListener {
            DroidGuardPreferences.setMode(it.context, Network)
            modeNetwork.editRequestFocus()
            updateConfiguration()
            true
        }
        modeNetwork.textChangedListener = {
            DroidGuardPreferences.setNetworkServerUrl(requireContext(), it)
        }
        modeEmbedded.isEnabled = !DroidGuardPreferences.isForcedLocalDisabled(requireContext())
        updateConfiguration()
    }

    fun updateConfiguration() {
        val mode = DroidGuardPreferences.getMode(requireContext())
        modeEmbedded.setIcon(if (mode == Embedded) ic_radio_checked else ic_radio_unchecked)
        modeNetwork.setIcon(if (mode == Network) ic_radio_checked else ic_radio_unchecked)
        modeNetwork.text = DroidGuardPreferences.getNetworkServerUrl(requireContext()) ?: ""
        modeNetwork.editable = mode == Network
        modeNetwork.hint = "https://example.com/droidguard/"
    }
}
