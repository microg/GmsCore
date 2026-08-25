/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.gms.R
import org.microg.gms.utils.getApplicationLabel
import org.microg.gms.vending.AllowType
import org.microg.gms.vending.InstallerData
import org.microg.gms.vending.VendingPreferences

class VendingInstallSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var switchBarPreference: SwitchBarPreference
    private lateinit var installers: PreferenceCategory
    private lateinit var none: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_vending_installer_settings)

        switchBarPreference = preferenceScreen.findPreference("pref_vending_allow_install_apps") ?: switchBarPreference
        installers = preferenceScreen.findPreference("pref_permission_installer_settings") ?: installers
        none = preferenceScreen.findPreference("pref_permission_installer_none") ?: none

        switchBarPreference.setOnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    VendingPreferences.setInstallEnabled(appContext, newValue)
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

    fun updateContent() {
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            installers.isVisible = VendingPreferences.isInstallEnabled(appContext)
            switchBarPreference.isChecked = VendingPreferences.isInstallEnabled(appContext)
            val installerList = VendingPreferences.getInstallerList(appContext)
            val installerDataSet = InstallerData.loadDataSet(installerList)
            val installerViews = installerDataSet.mapNotNull {
                runCatching {
                    SwitchPreference(appContext).apply {
                        key = "pref_permission_channels_${it.packageName}"
                        title = appContext.packageManager.getApplicationLabel(it.packageName)
                        icon = appContext.packageManager.getApplicationIcon(it.packageName)
                        isChecked = it.allowType == AllowType.ALLOW_ALWAYS.value
                        setOnPreferenceChangeListener { _, newValue ->
                            lifecycleScope.launchWhenResumed {
                                if (newValue is Boolean) {
                                    val allowType = if (newValue) AllowType.ALLOW_ALWAYS.value else AllowType.REJECT_ALWAYS.value
                                    val content = InstallerData.updateDataSetString(installerDataSet, it.apply { this.allowType = allowType })
                                    VendingPreferences.setInstallerList(appContext, content)
                                }
                            }
                            true
                        }
                    }
                }.getOrNull()
            }
            installers.removeAll()
            for (installerView in installerViews) {
                installers.addPreference(installerView)
            }
            if (installerViews.isEmpty()) {
                installers.addPreference(none)
            }
        }
    }
}