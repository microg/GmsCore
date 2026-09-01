/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.ui

import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.gms.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialSharedAxis
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.Constants
import org.microg.gms.gcm.ACTION_GCM_REGISTER_ALL_ACCOUNTS
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.Auth

val TWO_STATE_SETTINGS = listOf(
    Auth.TRUST_GOOGLE,
    Auth.VISIBLE,
    Auth.INCLUDE_ANDROID_ID,
    Auth.STRIP_DEVICE_NAME,
    Auth.TWO_STEP_VERIFICATION,
    Auth.FIND_DEVICES,
)

class PrivacyFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(MaterialColors.getColor(view, android.R.attr.colorBackground))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_privacy)
        val context = requireContext().applicationContext
        for (setting in TWO_STATE_SETTINGS) {
            findPreference<TwoStatePreference>(setting)?.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue is Boolean && preference.key in TWO_STATE_SETTINGS) {
                    SettingsContract.setSettings(context, Auth.getContentUri(context)) { put(preference.key, newValue) }
                    updateSettings()
                    if (preference.key == Auth.TWO_STEP_VERIFICATION && newValue) registerGcmInGms()
                    if (preference.key == Auth.FIND_DEVICES && newValue) registerGcmInGms()
                    if (preference.key == Auth.VISIBLE && Build.VERSION.SDK_INT >= 26) {
                        val am = AccountManager.get(context)
                        for (account in am.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)) {
                            am.setAccountVisibility(
                                account,
                                AccountManager.PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE,
                                if (newValue) AccountManager.VISIBILITY_VISIBLE else AccountManager.VISIBILITY_NOT_VISIBLE
                            )
                        }
                    }
                    true
                } else false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSettings()
    }

    private fun updateSettings() {
        val context = requireContext()
        for (setting in TWO_STATE_SETTINGS) {
            findPreference<TwoStatePreference>(setting)?.isChecked =
                SettingsContract.getSettings(context, Auth.getContentUri(context), arrayOf(setting)) { c -> c.getInt(0) != 0 }
        }
        if (Build.VERSION.SDK_INT < 26) {
            findPreference<Preference>(Auth.VISIBLE)?.isVisible = false
        }
    }

    private fun registerGcmInGms() {
        Intent(ACTION_GCM_REGISTER_ALL_ACCOUNTS).apply {
            `package` = Constants.GMS_PACKAGE_NAME
        }.let { requireContext().sendBroadcast(it) }
    }
}
