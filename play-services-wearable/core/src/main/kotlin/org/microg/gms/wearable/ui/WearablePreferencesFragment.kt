/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.microg.gms.ui.SwitchBarPreference
import org.microg.gms.wearable.WearableNotificationListenerService
import org.microg.gms.wearable.WearablePreferences
import org.microg.gms.wearable.core.R

class WearablePreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var notifications: SwitchBarPreference
    private lateinit var media: androidx.preference.SwitchPreferenceCompat
    private lateinit var calls: androidx.preference.SwitchPreferenceCompat
    private lateinit var listenerAccess: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_wearable)
    }

    override fun onBindPreferences() {
        notifications = preferenceScreen.findPreference(PREF_NOTIFICATIONS) ?: notifications
        media = preferenceScreen.findPreference(PREF_MEDIA) ?: media
        calls = preferenceScreen.findPreference(PREF_CALLS) ?: calls
        listenerAccess = preferenceScreen.findPreference(PREF_LISTENER) ?: listenerAccess
        notifications.setOnPreferenceChangeListener { _, newValue ->
            WearablePreferences.setNotificationsEnabled(requireContext(), newValue as Boolean)
            true
        }
        media.setOnPreferenceChangeListener { _, newValue ->
            WearablePreferences.setMediaControlEnabled(requireContext(), newValue as Boolean)
            true
        }
        calls.setOnPreferenceChangeListener { _, newValue ->
            WearablePreferences.setCallControlEnabled(requireContext(), newValue as Boolean)
            true
        }
        listenerAccess.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            true
        }
    }

    override fun onResume() {
        super.onResume()
        notifications.isChecked = WearablePreferences.isNotificationsEnabled(requireContext())
        media.isChecked = WearablePreferences.isMediaControlEnabled(requireContext())
        calls.isChecked = WearablePreferences.isCallControlEnabled(requireContext())
        listenerAccess.summary = getString(
            if (isNotificationListenerEnabled()) {
                org.microg.gms.base.core.R.string.service_status_enabled_short
            } else {
                R.string.wearable_pref_notification_access_summary
            }
        )
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(requireContext(), WearableNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it) == cn }
    }

    companion object {
        private const val PREF_NOTIFICATIONS = "pref_wearable_notifications"
        private const val PREF_MEDIA = "pref_wearable_media"
        private const val PREF_CALLS = "pref_wearable_calls"
        private const val PREF_LISTENER = "pref_wearable_notification_access"
    }
}
