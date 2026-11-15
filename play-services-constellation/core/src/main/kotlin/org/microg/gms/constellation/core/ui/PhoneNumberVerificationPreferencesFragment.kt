/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.microg.gms.constellation.core.ConstellationStateStore
import org.microg.gms.constellation.core.R
import org.microg.gms.ui.SwitchBarPreference

class PhoneNumberVerificationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var enabled: SwitchBarPreference
    private lateinit var sms: SwitchPreferenceCompat
    private lateinit var lastUseCategory: PreferenceCategory
    private lateinit var app: PhoneNumberVerificationAppPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_phone_number_verification)
        enabled = requireNotNull(findPreference(PREF_ENABLED))
        sms = requireNotNull(findPreference("pref_phone_number_verification_sms"))
        lastUseCategory = requireNotNull(findPreference(PREF_LAST_USE_CATEGORY))
        app = requireNotNull(findPreference(PREF_LAST_USE))
        enabled.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true && !hasPermissions(PHONE_PERMISSIONS)) {
                requestPermissions(PHONE_PERMISSIONS, REQUEST_PHONE)
                return@setOnPreferenceChangeListener false
            }
            ConstellationStateStore.setPhoneNumberVerificationEnabled(
                requireContext(),
                newValue as Boolean
            )
            sms.isEnabled = newValue
            true
        }
        sms.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                requestPermissions(SMS_PERMISSIONS, REQUEST_SMS)
            } else {
                startActivity(android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${requireContext().packageName}")
                ))
            }
            false
        }
    }

    private fun hasPermissions(permissions: Array<String>) = permissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PHONE && hasPermissions(PHONE_PERMISSIONS)) {
            ConstellationStateStore.setPhoneNumberVerificationEnabled(requireContext(), true)
            enabled.isChecked = true
            sms.isEnabled = true
        }
        sms.isChecked = hasPermissions(SMS_PERMISSIONS)
    }

    override fun onResume() {
        super.onResume()
        enabled.isChecked =
            ConstellationStateStore.isPhoneNumberVerificationEnabled(requireContext())
        sms.isEnabled = enabled.isChecked
        sms.isChecked = hasPermissions(SMS_PERMISSIONS)
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
        private const val REQUEST_PHONE = 1
        private const val REQUEST_SMS = 2
        private val PHONE_PERMISSIONS = arrayOf(Manifest.permission.READ_PHONE_STATE)
        private val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS
        )
        private const val PREF_ENABLED = "pref_phone_number_verification_enabled"
        private const val PREF_LAST_USE_CATEGORY = "prefcat_phone_number_verification_last_use"
        private const val PREF_LAST_USE = "pref_phone_number_verification_last_use"
    }
}
