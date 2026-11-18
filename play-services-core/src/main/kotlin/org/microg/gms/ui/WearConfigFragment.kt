/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.wearable.ConfigurationDatabaseHelper

class WearConfigFragment : PreferenceFragmentCompat() {

    private lateinit var appHeadingPreference: AppHeadingPreference
    private lateinit var configNamePreference: EditTextPreference
    private lateinit var configEnabledPreference: SwitchPreferenceCompat
    private lateinit var configAddressPreference: EditTextPreference
    private lateinit var configPackageNamePreference: EditTextPreference
    private lateinit var configDeletePreference: Preference

    private lateinit var database: ConfigurationDatabaseHelper
    private val configName: String?
        get() = arguments?.getString("name")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = ConfigurationDatabaseHelper(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_wear_config)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        appHeadingPreference =
            preferenceScreen.findPreference("pref_wear_config_heading") ?: appHeadingPreference
        configNamePreference =
            preferenceScreen.findPreference("pref_wear_config_name") ?: configNamePreference
        configEnabledPreference =
            preferenceScreen.findPreference("pref_wear_config_enabled") ?: configEnabledPreference
        configAddressPreference =
            preferenceScreen.findPreference("pref_wear_config_address") ?: configAddressPreference
        configPackageNamePreference =
            preferenceScreen.findPreference("pref_wear_config_package_name")
                ?: configPackageNamePreference
        configDeletePreference = preferenceScreen.findPreference("pref_wear_config_delete")
            ?: configDeletePreference

        configEnabledPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val config = database.getConfiguration(configName)
                config.enabled = newValue as Boolean
                database.putConfiguration(config)
                database.close()
                true
            }

        configAddressPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val config = database.getConfiguration(configName)
                config.address = newValue as String
                database.putConfiguration(config)
                database.close()
                true
            }

        configPackageNamePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                Log.d(TAG, "PackageName changed: $newValue")
                val config = database.getConfiguration(configName)
                config.packageName = newValue as String
                database.putConfiguration(config)
                database.close()
                true
            }

        configDeletePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showDeleteConfirm()
            true
        }
    }

    private fun showDeleteConfirm() {
        requireContext().buildAlertDialog()
            .setTitle(getString(R.string.wear_delete_confirm_title, configName))
            .setPositiveButton(android.R.string.yes) { _, _ -> delete() }
            .setNegativeButton(android.R.string.no) { _, _ -> }
            .show()
    }

    private fun delete() {
        lifecycleScope.launchWhenResumed {
            withContext(Dispatchers.IO) {
                database.deleteConfiguration(configName)
                database.close()
                // TODO: Leave fragment
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            configNamePreference.text = configName
            val config =
                configName?.let { database.getConfiguration(it) } ?: return@launchWhenResumed
            appHeadingPreference.packageName = config.packageName
            configEnabledPreference.isChecked = config.enabled
            configAddressPreference.text = config.address
            configPackageNamePreference.text = config.packageName

            database.close()
        }
    }
}
