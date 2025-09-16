/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.R
import org.microg.gms.auth.AuthConstants
import org.microg.gms.games.GameProfileSettings
import org.microg.gms.games.GamesConfigurationService
import org.microg.gms.games.ui.GamePlayDataActivity
import org.microg.gms.settings.SettingsContract

class GameProfileFragment : PreferenceFragmentCompat() {

    private lateinit var autoCreatePlayerEnabled: SwitchBarPreference
    private lateinit var allowUploadGamePlayed: SwitchPreferenceCompat
    private lateinit var changeDefaultAccountPreference: Preference
    private lateinit var deletePlayGamesAccountPreference: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_game_profile)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {

        autoCreatePlayerEnabled = preferenceScreen.findPreference(SettingsContract.GameProfile.ALLOW_CREATE_PLAYER) ?: autoCreatePlayerEnabled
        autoCreatePlayerEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    GameProfileSettings.setAllowCreatePlayer(requireContext(), newValue)
                }
                updateContent()
            }
            true
        }

        allowUploadGamePlayed = preferenceScreen.findPreference(SettingsContract.GameProfile.ALLOW_UPLOAD_GAME_PLAYED) ?: allowUploadGamePlayed
        allowUploadGamePlayed.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    GameProfileSettings.setAllowUploadGamePlayed(requireContext(), newValue)
                }
                updateContent()
            }
            true
        }

        changeDefaultAccountPreference = preferenceScreen.findPreference(PREF_CHANGE_DEFAULT_ACCOUNT) ?: changeDefaultAccountPreference
        changeDefaultAccountPreference.setOnPreferenceClickListener {
            GamePlayDataActivity.createIntent(requireContext(), GamePlayDataActivity.TYPE_CHANGED)
            true
        }

        deletePlayGamesAccountPreference = preferenceScreen.findPreference(PREF_DELETE_GAME_ACCOUNT) ?: deletePlayGamesAccountPreference
        deletePlayGamesAccountPreference.setOnPreferenceClickListener {
            GamePlayDataActivity.createIntent(requireContext(), GamePlayDataActivity.TYPE_DELETED)
            true
        }

        updateContent()
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            autoCreatePlayerEnabled.isChecked = GameProfileSettings.getAllowCreatePlayer(requireContext())
            allowUploadGamePlayed.isChecked = GameProfileSettings.getAllowUploadGamePlayed(requireContext())

            val hasGamePlayAccount = runCatching {
                AccountManager.get(requireContext()).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
            }.getOrNull()?.any { GamesConfigurationService.getPlayer(requireContext(), it) != null }
            deletePlayGamesAccountPreference.isVisible = hasGamePlayAccount == true
            changeDefaultAccountPreference.isVisible = hasGamePlayAccount == true
        }
    }

    companion object {
        private const val PREF_CHANGE_DEFAULT_ACCOUNT = "pref_change_default_account"
        private const val PREF_DELETE_GAME_ACCOUNT = "pref_delete_game_account"
    }

}