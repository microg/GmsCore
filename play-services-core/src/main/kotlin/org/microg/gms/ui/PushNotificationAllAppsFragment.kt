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
        val context = requireContext()
        lifecycleScope.launchWhenResumed {
            val apps = withContext(Dispatchers.IO) {
                val res = database.appList.map { app ->
                    val pref = AppIconPreference(context)
                    pref.packageName = app.packageName
                    pref.summary = when {
                        app.lastMessageTimestamp > 0 -> getString(R.string.gcm_last_message_at, DateUtils.getRelativeTimeSpanString(app.lastMessageTimestamp))
                        else -> null
                    }
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openGcmAppDetailsFromAll, bundleOf(
                                "package" to app.packageName
                        ))
                        true
                    }
                    pref.key = "pref_push_app_" + app.packageName
                    pref to (database.getRegistrationsByApp(app.packageName))
                }.sortedBy {
                    it.first.title.toString().lowercase()
                }.mapIndexed { idx, pair ->
                    pair.first.order = idx
                    pair
                }
                database.close()
                res
            }
            registered.removeAll()
            unregistered.removeAll()

            var hadRegistered = false
            var hadUnregistered = false

            val registeredList = mutableListOf<Preference>()
            val unregisteredList = mutableListOf<Preference>()

            for ((pref, registrations) in apps) {
                if (registrations.isEmpty()) {
                    unregisteredList.add(pref)
                    hadUnregistered = true
                } else {
                    registeredList.add(pref)
                    hadRegistered = true
                }
            }

            if (!hadRegistered) registeredList.add(registeredNone)
            if (!hadUnregistered) unregisteredList.add(unregisteredNone)

            registeredList.forEachIndexed { index, pref ->
                pref.layoutResource = chooseLayoutForPosition(index, registeredList.size)
                registered.addPreference(pref)
            }

            unregisteredList.forEachIndexed { index, pref ->
                pref.layoutResource = chooseLayoutForPosition(index, unregisteredList.size)
                unregistered.addPreference(pref)
            }

            registered.isVisible = true
            unregistered.isVisible = true
            progress.isVisible = false
        }
    }

    private fun chooseLayoutForPosition(index: Int, total: Int): Int {
        return when {
            total <= 1 -> R.layout.preference_material_secondary_single
            total == 2 -> if (index == 0) {
                R.layout.preference_material_secondary_top
            } else {
                R.layout.preference_material_secondary_bottom
            }

            else -> when (index) {
                0 -> R.layout.preference_material_secondary_top
                total - 1 -> R.layout.preference_material_secondary_bottom
                else -> R.layout.preference_material_secondary_middle
            }
        }
    }
}
