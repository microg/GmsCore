/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.content.res.AppCompatResources
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

class PushNotificationAllAppsFragment : PreferenceFragmentCompat() {
    private lateinit var database: GcmDatabase
    private lateinit var registered: PreferenceCategory
    private lateinit var unregistered: PreferenceCategory
    private lateinit var registeredNone: Preference
    private lateinit var unregisteredNone: Preference
    private lateinit var progress: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = GcmDatabase(context)
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_push_notifications_all_apps)
        registered = preferenceScreen.findPreference("prefcat_push_apps_registered") ?: registered
        unregistered = preferenceScreen.findPreference("prefcat_push_apps_unregistered") ?: unregistered
        registeredNone = preferenceScreen.findPreference("pref_push_apps_registered_none") ?: registeredNone
        unregisteredNone = preferenceScreen.findPreference("pref_push_apps_unregistered_none") ?: unregisteredNone
        progress = preferenceScreen.findPreference("pref_push_apps_all_progress") ?: progress
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            val apps = withContext(Dispatchers.IO) {
                val res = database.appList.map { app ->
                    app to context.packageManager.getApplicationInfoIfExists(app.packageName)
                }.map { (app, applicationInfo) ->
                    val pref = AppIconPreference(context)
                    pref.title = applicationInfo?.loadLabel(context.packageManager) ?: app.packageName
                    pref.summary = when {
                        app.lastMessageTimestamp > 0 -> getString(R.string.gcm_last_message_at, DateUtils.getRelativeTimeSpanString(app.lastMessageTimestamp))
                        else -> null
                    }
                    pref.icon = applicationInfo?.loadIcon(context.packageManager)
                            ?: AppCompatResources.getDrawable(context, android.R.mipmap.sym_def_app_icon)
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openGcmAppDetailsFromAll, bundleOf(
                                "package" to app.packageName
                        ))
                        true
                    }
                    pref.key = "pref_push_app_" + app.packageName
                    pref to (database.getRegistrationsByApp(app.packageName))
                }.sortedBy {
                    it.first.title.toString().toLowerCase()
                }.mapIndexed { idx, pair ->
                    pair.first.order = idx
                    pair
                }
                database.close()
                res
            }
            registered.removeAll()
            registered.isVisible = true
            unregistered.removeAll()
            unregistered.isVisible = true

            var hadRegistered = false
            var hadUnregistered = false

            for (pair in apps) {
                if (pair.second.isEmpty()) {
                    unregistered.addPreference(pair.first)
                    hadUnregistered = true
                } else {
                    registered.addPreference(pair.first)
                    hadRegistered = true
                }
            }

            registeredNone.isVisible = !hadRegistered
            unregisteredNone.isVisible = !hadUnregistered
            if (!hadRegistered) registered.addPreference(registeredNone)
            if (!hadUnregistered) unregistered.addPreference(unregisteredNone)
            progress.isVisible = false
        }
    }
}
