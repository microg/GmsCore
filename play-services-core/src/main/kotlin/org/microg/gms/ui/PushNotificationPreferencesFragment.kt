/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.getGcmServiceInfo

class PushNotificationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var pushStatusCategory: PreferenceCategory
    private lateinit var pushStatus: Preference
    private lateinit var pushApps: PreferenceCategory
    private lateinit var pushAppsAll: Preference
    private lateinit var pushAppsNone: Preference
    private lateinit var database: GcmDatabase
    private val handler = Handler()
    private val updateRunnable = Runnable { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = GcmDatabase(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_push_notifications)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        pushStatusCategory = preferenceScreen.findPreference("prefcat_push_status") ?: pushStatusCategory
        pushStatus = preferenceScreen.findPreference("pref_push_status") ?: pushStatus
        pushApps = preferenceScreen.findPreference("prefcat_push_apps") ?: pushApps
        pushAppsAll = preferenceScreen.findPreference("pref_push_apps_all") ?: pushAppsAll
        pushAppsNone = preferenceScreen.findPreference("pref_push_apps_none") ?: pushAppsNone
        pushAppsAll.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAllGcmApps)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateContent()
    }

    override fun onPause() {
        super.onPause()
        database.close()
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateStatus() {
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL)
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenStarted {
            val statusInfo = getGcmServiceInfo(appContext)
            pushStatusCategory.isVisible = statusInfo != null && statusInfo.configuration.enabled
            pushStatus.summary = if (statusInfo != null && statusInfo.connected) {
                appContext.getString(R.string.gcm_network_state_connected, DateUtils.getRelativeTimeSpanString(statusInfo.startTimestamp, System.currentTimeMillis(), 0))
            } else {
                appContext.getString(R.string.gcm_network_state_disconnected)
            }
        }
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            val (apps, showAll) = withContext(Dispatchers.IO) {
                val apps = database.appList.sortedByDescending { it.lastMessageTimestamp }
                val res = apps.map { app ->
                    app to context.packageManager.getApplicationInfoIfExists(app.packageName)
                }.mapNotNull { (app, info) ->
                    if (info == null) null else app to info
                }.take(3).mapIndexed { idx, (app, applicationInfo) ->
                    val pref = AppIconPreference(context)
                    pref.order = idx
                    pref.title = applicationInfo.loadLabel(context.packageManager)
                    pref.icon = applicationInfo.loadIcon(context.packageManager)
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openGcmAppDetails, bundleOf(
                                "package" to app.packageName
                        ))
                        true
                    }
                    pref.key = "pref_push_app_" + app.packageName
                    pref
                }.let { it to (it.size < apps.size) }
                database.close()
                res
            }
            pushAppsAll.isVisible = showAll
            pushApps.removeAll()
            for (app in apps) {
                pushApps.addPreference(app)
            }
            if (showAll) {
                pushApps.addPreference(pushAppsAll)
            } else if (apps.isEmpty()) {
                pushApps.addPreference(pushAppsNone)
            }
        }
    }

    companion object {
        private const val UPDATE_INTERVAL = 1000L
    }
}
