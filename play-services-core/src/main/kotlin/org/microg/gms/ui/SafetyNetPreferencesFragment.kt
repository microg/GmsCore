/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.safetynet.SafetyNetDatabase
import org.microg.gms.safetynet.SafetyNetRequestType
import org.microg.gms.safetynet.SafetyNetRequestType.ATTESTATION
import org.microg.gms.safetynet.SafetyNetRequestType.RECAPTCHA
import kotlin.random.Random

class SafetyNetPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var runAttest: Preference
    private lateinit var runReCaptcha: Preference
    private lateinit var apps: PreferenceCategory
    private lateinit var appsAll: Preference
    private lateinit var appsNone: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_safetynet)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        runAttest = preferenceScreen.findPreference("pref_safetynet_run_attest") ?: runAttest
        runReCaptcha = preferenceScreen.findPreference("pref_recaptcha_run_test") ?: runReCaptcha
        apps = preferenceScreen.findPreference("prefcat_safetynet_apps") ?: apps
        appsAll = preferenceScreen.findPreference("pref_safetynet_apps_all") ?: appsAll
        appsNone = preferenceScreen.findPreference("pref_safetynet_apps_none") ?: appsNone

        runAttest.setOnPreferenceClickListener {
            val context = context ?: return@setOnPreferenceClickListener false
            runAttest.setIcon(R.drawable.ic_circle_pending)
            runAttest.setSummary(R.string.pref_test_summary_running)
            lifecycleScope.launchWhenResumed {
                val response = SafetyNet.getClient(requireActivity())
                    .attest(Random.nextBytes(32), "AIzaSyCcJO6IZiA5Or_AXw3LFdaTCmpnfL4pJ-Q").await()
                val (_, payload, _) = try {
                    response.jwsResult.split(".")
                } catch (e: Exception) {
                    listOf(null, null, null)
                }
                formatSummaryForSafetyNetResult(
                    context,
                    payload?.let { Base64.decode(it, Base64.URL_SAFE).decodeToString() },
                    response.result.status,
                    ATTESTATION
                )
                    .let { (summary, icon) ->
                        runAttest.summary = summary
                        runAttest.icon = icon
                    }
                updateContent()
            }
            true
        }
        runReCaptcha.setOnPreferenceClickListener {
            val context = context ?: return@setOnPreferenceClickListener false
            runReCaptcha.setIcon(R.drawable.ic_circle_pending)
            runReCaptcha.setSummary(R.string.pref_test_summary_running)
            lifecycleScope.launchWhenResumed {
                val response = SafetyNet.getClient(requireActivity())
                    .verifyWithRecaptcha("6Lc4TzgeAAAAAJnW7Jbo6UtQ0xGuTKjHAeyhINuq").await()
                formatSummaryForSafetyNetResult(context, response.tokenResult, response.result.status, RECAPTCHA)
                    .let { (summary, icon) ->
                        runReCaptcha.summary = summary
                        runReCaptcha.icon = icon
                    }
                updateContent()
            }
            true
        }
        appsAll.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAllSafetyNetApps)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            val (apps, showAll) = withContext(Dispatchers.IO) {
                val apps = SafetyNetDatabase(requireContext()).use { it.recentApps }
                apps.map { app ->
                    app to context.packageManager.getApplicationInfoIfExists(app.first)
                }.mapNotNull { (app, info) ->
                    if (info == null) null else app to info
                }.take(3).mapIndexed { idx, (app, applicationInfo) ->
                    val pref = AppIconPreference(context)
                    pref.order = idx
                    pref.title = applicationInfo.loadLabel(context.packageManager)
                    pref.icon = applicationInfo.loadIcon(context.packageManager)
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(
                            requireContext(), R.id.openSafetyNetAppDetails, bundleOf(
                                "package" to app.first
                            )
                        )
                        true
                    }
                    pref.key = "pref_safetynet_app_" + app.first
                    pref
                }.let { it to (it.size < apps.size) }
            }
            appsAll.isVisible = showAll
            this@SafetyNetPreferencesFragment.apps.removeAll()
            for (app in apps) {
                this@SafetyNetPreferencesFragment.apps.addPreference(app)
            }
            if (showAll) {
                this@SafetyNetPreferencesFragment.apps.addPreference(appsAll)
            } else if (apps.isEmpty()) {
                this@SafetyNetPreferencesFragment.apps.addPreference(appsNone)
            }

        }
    }
}
