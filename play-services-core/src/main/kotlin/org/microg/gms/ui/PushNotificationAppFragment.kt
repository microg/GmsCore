/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.PushRegisterManager

class PushNotificationAppFragment : PreferenceFragmentCompat() {
    private lateinit var appHeadingPreference: AppHeadingPreference
    private lateinit var wakeForDelivery: TwoStatePreference
    private lateinit var allowRegister: TwoStatePreference
    private lateinit var status: Preference
    private lateinit var unregister: Preference
    private lateinit var unregisterCat: PreferenceCategory

    private lateinit var database: GcmDatabase
    private val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_push_notifications_app)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = GcmDatabase(context)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        appHeadingPreference = preferenceScreen.findPreference("pref_push_app_heading") ?: appHeadingPreference
        wakeForDelivery = preferenceScreen.findPreference("pref_push_app_wake_for_delivery") ?: wakeForDelivery
        allowRegister = preferenceScreen.findPreference("pref_push_app_allow_register") ?: allowRegister
        unregister = preferenceScreen.findPreference("pref_push_app_unregister") ?: unregister
        unregisterCat = preferenceScreen.findPreference("prefcat_push_app_unregister") ?: unregisterCat
        status = preferenceScreen.findPreference("pref_push_app_status") ?: status
        wakeForDelivery.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            database.setAppWakeForDelivery(packageName, newValue as Boolean)
            database.close()
            true
        }
        allowRegister.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as? Boolean ?: return@OnPreferenceChangeListener false
            if (!enabled) {
                val registrations = packageName?.let { database.getRegistrationsByApp(it) } ?: emptyList()
                if (registrations.isNotEmpty()) {
                    showUnregisterConfirm(R.string.gcm_unregister_after_deny_message)
                }
            }
            database.setAppAllowRegister(packageName, enabled)
            database.close()
            true
        }
        unregister.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showUnregisterConfirm(R.string.gcm_unregister_confirm_message)
            true
        }
    }


    private fun showUnregisterConfirm(unregisterConfirmDesc: Int) {
        val pm = requireContext().packageManager
        val applicationInfo = pm.getApplicationInfoIfExists(packageName)
        requireContext().buildAlertDialog()
                .setTitle(getString(R.string.gcm_unregister_confirm_title, applicationInfo?.loadLabel(pm)
                        ?: packageName))
                .setMessage(unregisterConfirmDesc)
                .setPositiveButton(android.R.string.yes) { _, _ -> unregister() }
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .show()
    }

    private fun unregister() {
        lifecycleScope.launchWhenResumed {
            withContext(Dispatchers.IO) {
                for (registration in database.getRegistrationsByApp(packageName)) {
                    PushRegisterManager.unregister(context, registration.packageName, registration.signature, null, null)
                }
            }
            updateDetails()
        }
    }

    override fun onResume() {
        super.onResume()
        updateDetails()
    }

    private fun updateDetails() {
        lifecycleScope.launchWhenResumed {
            appHeadingPreference.packageName = packageName
            val app = packageName?.let { database.getApp(it) }
            wakeForDelivery.isChecked = app?.wakeForDelivery ?: true
            allowRegister.isChecked = app?.allowRegister ?: true
            val registrations = packageName?.let { database.getRegistrationsByApp(it) } ?: emptyList()
            unregisterCat.isVisible = registrations.isNotEmpty()

            val sb = StringBuilder()
            if ((app?.totalMessageCount ?: 0L) == 0L) {
                sb.append(getString(R.string.gcm_no_message_yet))
            } else {
                sb.append(getString(R.string.gcm_messages_counter, app?.totalMessageCount, app?.totalMessageBytes))
                if (app?.lastMessageTimestamp != 0L) {
                    sb.append("\n").append(getString(R.string.gcm_last_message_at, DateUtils.getRelativeDateTimeString(context, app?.lastMessageTimestamp ?: 0L, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME)))
                }
            }
            for (registration in registrations) {
                sb.append("\n")
                if (registration.timestamp == 0L) {
                    sb.append(getString(R.string.gcm_registered))
                } else {
                    sb.append(getString(R.string.gcm_registered_since, DateUtils.getRelativeDateTimeString(context, registration.timestamp, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME)))
                }
            }
            status.summary = sb.toString()

            database.close()
        }
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }
}
