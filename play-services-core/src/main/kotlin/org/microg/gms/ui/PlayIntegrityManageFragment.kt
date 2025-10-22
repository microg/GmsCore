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
import org.microg.gms.vending.IntegrityVisitData
import org.microg.gms.vending.VendingPreferences
import java.text.SimpleDateFormat
import java.util.Date

class PlayIntegrityManageFragment : PreferenceFragmentCompat() {
    private lateinit var switchBarPreference: SwitchBarPreference
    private lateinit var apps: PreferenceCategory
    private lateinit var appsNone: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_play_integrity)

        switchBarPreference = preferenceScreen.findPreference("pref_play_integrity_enabled") ?: switchBarPreference
        apps = preferenceScreen.findPreference("pref_play_integrity_apps") ?: apps
        appsNone = preferenceScreen.findPreference("pref_play_integrity_apps_none") ?: appsNone

        switchBarPreference.setOnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    VendingPreferences.setPlayIntegrityEnabled(appContext, newValue)
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
            switchBarPreference.isChecked = VendingPreferences.isPlayIntegrityEnabled(appContext)
            val visitAppContent = VendingPreferences.getPlayIntegrityAppList(appContext)
            val visitAppSet = IntegrityVisitData.loadDataSet(visitAppContent)
            val visitAppViews = visitAppSet.mapNotNull {
                runCatching {
                    SwitchPreference(appContext).apply {
                        key = "pref_permission_apps_${it.packageName}"
                        title = appContext.packageManager.getApplicationLabel(it.packageName)
                        icon = appContext.packageManager.getApplicationIcon(it.packageName)
                        isChecked = it.allowed
                        summary = "Last: ${it.lastVisitResult} \n${it.lastVisitTime?.let { time -> SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Date(time)) }}"
                        setOnPreferenceChangeListener { _, newValue ->
                            lifecycleScope.launchWhenResumed {
                                if (newValue is Boolean) {
                                    val content = IntegrityVisitData.updateDataSetString(visitAppSet, it.apply { this.allowed = newValue })
                                    VendingPreferences.setPlayIntegrityAppList(appContext, content)
                                }
                            }
                            true
                        }
                    }
                }.getOrNull()
            }
            apps.removeAll()
            for (visitAppView in visitAppViews) {
                apps.addPreference(visitAppView)
            }
            if (visitAppViews.isEmpty()) {
                apps.addPreference(appsNone)
            }
        }
    }
}