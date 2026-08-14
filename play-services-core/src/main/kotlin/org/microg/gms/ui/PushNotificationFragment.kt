/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.gcm.getGcmServiceInfo

class PushNotificationFragment : PreferenceFragmentCompat() {
    private lateinit var switchBarPreference: SwitchBarPreference
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
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        database = GcmDatabase(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(MaterialColors.getColor(view, android.R.attr.colorBackground))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_push_notifications)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        switchBarPreference = preferenceScreen.findPreference("pref_push_enabled") ?: switchBarPreference
        pushStatusCategory = preferenceScreen.findPreference("prefcat_push_status") ?: pushStatusCategory
        pushStatus = preferenceScreen.findPreference("pref_push_status") ?: pushStatus
        pushApps = preferenceScreen.findPreference("prefcat_push_apps") ?: pushApps
        pushAppsAll = preferenceScreen.findPreference("pref_push_apps_all") ?: pushAppsAll
        pushAppsNone = preferenceScreen.findPreference("pref_push_apps_none") ?: pushAppsNone
        pushAppsAll.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAllGcmApps)
            true
        }
        switchBarPreference.setOnPreferenceChangeListener { _, newValue ->
            val newStatus = newValue as Boolean
            GcmPrefs.setEnabled(requireContext(), newStatus)
            true
        }
    }

    override fun onResume() {
        super.onResume()

        switchBarPreference.isEnabled = CheckinPreferences.isEnabled(requireContext())
        switchBarPreference.isChecked = GcmPrefs.get(requireContext()).isEnabled

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
            switchBarPreference.isChecked = statusInfo.configuration.enabled
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
                    pref.applicationInfo = applicationInfo
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

            pushApps.removeAll()

            val totalDisplayed = when {
                apps.isEmpty() -> 1
                showAll -> apps.size + 1
                else -> apps.size
            }

            apps.forEachIndexed { index, pref ->
                pref.layoutResource = chooseLayoutForPosition(index, totalDisplayed)
                pref.isIconSpaceReserved = true
                pushApps.addPreference(pref)
            }

            if (apps.isEmpty()) {
                pushAppsNone.layoutResource = chooseLayoutForPosition(0, totalDisplayed)
                pushAppsNone.isIconSpaceReserved = false
                pushApps.addPreference(pushAppsNone)
            } else if (showAll) {
                pushAppsAll.layoutResource = chooseLayoutForPosition(apps.size, totalDisplayed)
                pushAppsAll.isIconSpaceReserved = false
                pushApps.addPreference(pushAppsAll)
            }
            pushAppsAll.isVisible = showAll
        }
    }

    private fun chooseLayoutForPosition(index: Int, total: Int): Int {
        return when {
            total <= 1 -> R.layout.preference_material_secondary_single
            total == 2 -> if (index == 0) R.layout.preference_material_secondary_top else R.layout.preference_material_secondary_bottom
            else -> when (index) {
                0 -> R.layout.preference_material_secondary_top
                total - 1 -> R.layout.preference_material_secondary_bottom
                else -> R.layout.preference_material_secondary_middle
            }
        }
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.gcm_menu_item, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                findNavController().navigate(requireContext(), R.id.openGcmAdvancedSettings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val UPDATE_INTERVAL = 1000L
    }
}
