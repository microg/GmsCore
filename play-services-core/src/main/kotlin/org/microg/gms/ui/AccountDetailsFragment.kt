/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.R
import kotlinx.coroutines.launch
import org.microg.gms.auth.AuthConstants
import org.microg.gms.location.reporting.ManagedAccountLocationSettings
import org.microg.gms.location.reporting.REPORTING_SETTINGS_CHANGED_ACTION

internal const val ARG_ACCOUNT_DETAILS_NAME = "accountName"

private const val PREF_ACCOUNT_DETAILS_HEADER = "pref_account_details_header"
private const val PREF_ACCOUNT_TIMELINE = "pref_account_timeline"
private const val PREF_ACCOUNT_TIMELINE_UPLOAD = "pref_account_timeline_upload"
private const val PREF_ACCOUNT_SYSTEM_SYNC = "pref_account_system_sync"

class AccountDetailsFragment : PreferenceFragmentCompat() {
    private lateinit var account: Account
    private lateinit var timeline: SwitchPreferenceCompat
    private lateinit var timelineUpload: SwitchPreferenceCompat
    private var accountSettings: ManagedAccountLocationSettings? = null
    private var requestId = 0
    private var requestInProgress = false
    private var refreshPending = false
    private val settingsChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != REPORTING_SETTINGS_CHANGED_ACTION) return
            if (requestInProgress) {
                refreshPending = true
            } else {
                loadSettings(forceRefresh = false)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_account_details, rootKey)
        account = Account(
            requireArguments().getString(ARG_ACCOUNT_DETAILS_NAME).orEmpty(),
            AuthConstants.DEFAULT_ACCOUNT_TYPE
        )
        timeline = requireNotNull(findPreference(PREF_ACCOUNT_TIMELINE))
        timelineUpload = requireNotNull(findPreference(PREF_ACCOUNT_TIMELINE_UPLOAD))

        findPreference<Preference>(PREF_ACCOUNT_DETAILS_HEADER)?.apply {
            title = account.name
            summary = getString(R.string.pref_account_type_google)
        }
        timeline.setOnPreferenceChangeListener { _, newValue ->
            val settings = accountSettings
            if (newValue is Boolean && settings != null) {
                updateSettings(
                    historyEnabled = newValue,
                    reportingEnabled = newValue && settings.reportingEnabled == true,
                    synchronizeHistoryEnabled = true
                )
            }
            false
        }
        timelineUpload.setOnPreferenceChangeListener { _, newValue ->
            val settings = accountSettings
            if (newValue is Boolean && settings != null) {
                updateSettings(
                    historyEnabled = settings.historyEnabled == true,
                    reportingEnabled = newValue,
                    synchronizeHistoryEnabled = false
                )
            }
            false
        }
        findPreference<Preference>(PREF_ACCOUNT_SYSTEM_SYNC)?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf(AuthConstants.DEFAULT_ACCOUNT_TYPE))
            })
            true
        }
        renderSettings(null, loading = true)
    }

    override fun onResume() {
        super.onResume()
        if (!AccountManager.get(requireContext())
                .getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
                .contains(account)) {
            findNavController().popBackStack()
            return
        }
        loadSettings(forceRefresh = true)
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            settingsChangedReceiver,
            IntentFilter(REPORTING_SETTINGS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        requireContext().unregisterReceiver(settingsChangedReceiver)
        super.onStop()
    }

    private fun loadSettings(forceRefresh: Boolean) {
        val context = requireContext().applicationContext
        val currentRequestId = ++requestId
        requestInProgress = true
        renderSettings(accountSettings, loading = true)
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = AccountLocationSettingsService.fetchSettings(
                context,
                account,
                forceRefresh = forceRefresh
            )
            if (currentRequestId != requestId) return@launch
            requestInProgress = false
            accountSettings = settings
            renderSettings(settings, loading = false)
            refreshIfPending()
        }
    }

    private fun updateSettings(
        historyEnabled: Boolean,
        reportingEnabled: Boolean,
        synchronizeHistoryEnabled: Boolean
    ) {
        val context = requireContext().applicationContext
        val currentRequestId = ++requestId
        requestInProgress = true
        renderSettings(accountSettings, loading = true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = AccountLocationSettingsService.updateSettings(
                context,
                account,
                historyEnabled,
                reportingEnabled,
                synchronizeHistoryEnabled
            )
            if (currentRequestId != requestId) return@launch
            requestInProgress = false
            result.settings?.let { accountSettings = it }
            renderSettings(accountSettings, loading = false)
            if (!result.success) {
                Toast.makeText(
                    requireContext(),
                    R.string.pref_account_timeline_update_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
            refreshIfPending()
        }
    }

    private fun refreshIfPending() {
        if (!refreshPending) return
        refreshPending = false
        loadSettings(forceRefresh = false)
    }

    private fun renderSettings(settings: ManagedAccountLocationSettings?, loading: Boolean) {
        val restricted = settings?.userRestriction?.let { it != 0 } == true
        val historyKnown = settings?.historyEnabled != null
        val canDisableUpload = settings?.reportingEnabled == true
        val canEnableUpload = settings?.historyEnabled == true && !restricted
        timeline.isChecked = settings?.timelineEnabled == true
        timelineUpload.isChecked = settings?.reportingEnabled == true
        timeline.isEnabled = !loading && historyKnown && !restricted
        timelineUpload.isEnabled = !loading && settings != null &&
                (canDisableUpload || canEnableUpload)
        timeline.summary = when {
            loading -> getString(R.string.pref_account_timeline_loading)
            !historyKnown -> getString(R.string.pref_account_timeline_unavailable)
            restricted -> getString(R.string.pref_account_timeline_restricted)
            else -> getString(R.string.pref_account_timeline_summary)
        }
        timelineUpload.summary = when {
            loading -> getString(R.string.pref_account_timeline_loading)
            !historyKnown -> getString(R.string.pref_account_timeline_unavailable)
            restricted && !canDisableUpload -> getString(R.string.pref_account_timeline_restricted)
            else -> getString(R.string.pref_account_timeline_upload_summary)
        }
    }
}
