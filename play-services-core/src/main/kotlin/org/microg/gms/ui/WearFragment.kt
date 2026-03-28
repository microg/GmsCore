/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.wearable.ConfigurationDatabaseHelper

class WearFragment : PreferenceFragmentCompat() {

    private lateinit var wearConnections: PreferenceCategory
    private lateinit var wearConnectionsAll: Preference
    private lateinit var wearConnectionsNone: Preference
    private lateinit var database: ConfigurationDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = ConfigurationDatabaseHelper(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_wear)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        wearConnections =
            preferenceScreen.findPreference("prefcat_wear_connections") ?: wearConnections
        wearConnectionsAll =
            preferenceScreen.findPreference("pref_wear_connections_all") ?: wearConnectionsAll
        wearConnectionsNone =
            preferenceScreen.findPreference("pref_wear_connections_none") ?: wearConnectionsNone
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()

            val (configs, showAll) = withContext(Dispatchers.IO) {
                val configs = database.allConfigurations
                val res = configs.take(3).mapIndexed { idx, config ->
                    val pref = WearConfigPreference(context)
                    pref.order = idx
                    pref.connectionConfig = config
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openWearConfigDetails, bundleOf(
                            "name" to config.name
                        ))
                        true
                    }
                    pref.key = "pref_wear_conection_" + config.packageName
                    pref
                }.let { it to (it.size < configs.size) }
                database.close()
                res
            }

            wearConnectionsAll.isVisible = showAll
            wearConnections.removeAll()
            for (config in configs) {
                wearConnections.addPreference(config)
            }
            if (showAll) {
                wearConnections.addPreference(wearConnectionsAll)
            } else if (configs.isEmpty()) {
                wearConnections.addPreference(wearConnectionsNone)
            }
        }
    }
}
