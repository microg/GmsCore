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

class VendingFragment : PreferenceFragmentCompat() {
    private lateinit var licensingEnabled: TwoStatePreference
    private lateinit var licensingPurchaseFreeAppsEnabled: TwoStatePreference
    private lateinit var iapEnable: TwoStatePreference
    private lateinit var assetDeliveryEnabled: TwoStatePreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_vending)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        licensingEnabled = preferenceScreen.findPreference(PREF_LICENSING_ENABLED) ?: licensingEnabled
        licensingEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    VendingPreferences.setLicensingEnabled(appContext, newValue)
                }
                updateContent()
            }
            true
        }

        licensingPurchaseFreeAppsEnabled = preferenceScreen.findPreference(PREF_LICENSING_PURCHASE_FREE_APPS_ENABLED) ?: licensingPurchaseFreeAppsEnabled
        licensingPurchaseFreeAppsEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    VendingPreferences.setLicensingPurchaseFreeAppsEnabled(appContext, newValue)
                }
                updateContent()
            }
            true
        }

        iapEnable = preferenceScreen.findPreference(PREF_IAP_ENABLED) ?: iapEnable
        iapEnable.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    VendingPreferences.setBillingEnabled(appContext, newValue)
                }
                updateContent()
            }
            true
        }

        assetDeliveryEnabled = preferenceScreen.findPreference(PREF_ASSET_DELIVERY_ENABLED) ?: assetDeliveryEnabled
        assetDeliveryEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val appContext = requireContext().applicationContext
            lifecycleScope.launchWhenResumed {
                if (newValue is Boolean) {
                    VendingPreferences.setAssetDeliveryEnabled(appContext, newValue)
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
            licensingEnabled.isChecked = VendingPreferences.isLicensingEnabled(appContext)
            licensingPurchaseFreeAppsEnabled.isChecked = VendingPreferences.isLicensingPurchaseFreeAppsEnabled(appContext)
            iapEnable.isChecked = VendingPreferences.isBillingEnabled(appContext)
            assetDeliveryEnabled.isChecked = VendingPreferences.isAssetDeliveryEnabled(appContext)
        }
    }

    companion object {
        const val PREF_LICENSING_ENABLED = "vending_licensing"
        const val PREF_LICENSING_PURCHASE_FREE_APPS_ENABLED = "vending_licensing_purchase_free_apps"
        const val PREF_IAP_ENABLED = "vending_iap"
        const val PREF_ASSET_DELIVERY_ENABLED = "vending_asset_delivery"
    }
}