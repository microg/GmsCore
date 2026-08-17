/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.BuildConfig
import com.google.android.gms.R
import com.google.android.gms.recaptcha.Recaptcha
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaActionType
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.droidguard.core.DroidGuardPreferences
import org.microg.gms.safetynet.SafetyNetDatabase
import org.microg.gms.safetynet.SafetyNetPreferences
import org.microg.gms.safetynet.SafetyNetRequestType.*
import org.microg.gms.utils.singleInstanceOf
import org.microg.gms.vending.PlayIntegrityData
import org.microg.gms.vending.VendingPreferences
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class SafetyNetFragment : PreferenceFragmentCompat() {
    private lateinit var switchBarPreference: SwitchBarPreference
    private lateinit var runAttest: Preference
    private lateinit var runReCaptcha: Preference
    private lateinit var runReCaptchaEnterprise: Preference
    private lateinit var apps: PreferenceCategory
    private lateinit var appsAll: Preference
    private lateinit var appsNone: Preference
    private lateinit var droidguardUnsupported: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_safetynet)

        switchBarPreference = preferenceScreen.findPreference("pref_safetynet_enabled") ?: switchBarPreference
        runAttest = preferenceScreen.findPreference("pref_safetynet_run_attest") ?: runAttest
        runReCaptcha = preferenceScreen.findPreference("pref_recaptcha_run_test") ?: runReCaptcha
        runReCaptchaEnterprise = preferenceScreen.findPreference("pref_recaptcha_enterprise_run_test") ?: runReCaptchaEnterprise
        apps = preferenceScreen.findPreference("prefcat_safetynet_apps") ?: apps
        appsAll = preferenceScreen.findPreference("pref_safetynet_apps_all") ?: appsAll
        appsNone = preferenceScreen.findPreference("pref_safetynet_apps_none") ?: appsNone
        droidguardUnsupported = preferenceScreen.findPreference("pref_droidguard_unsupported") ?: droidguardUnsupported

        runAttest.isVisible = SAFETYNET_API_KEY != null
        runReCaptcha.isVisible = RECAPTCHA_SITE_KEY != null
        runReCaptchaEnterprise.isVisible = RECAPTCHA_ENTERPRISE_SITE_KEY != null

        runAttest.setOnPreferenceClickListener { runSafetyNetAttest(); true }
        runReCaptcha.setOnPreferenceClickListener { runReCaptchaAttest(); true }
        runReCaptchaEnterprise.setOnPreferenceClickListener { runReCaptchaEnterpriseAttest();true }
        appsAll.setOnPreferenceClickListener { findNavController().navigate(requireContext(), R.id.openAllSafetyNetApps);true }
        switchBarPreference.setOnPreferenceChangeListener { _, newValue ->
            val newStatus = newValue as Boolean
            SafetyNetPreferences.setEnabled(requireContext(), newStatus)
            DroidGuardPreferences.setEnabled(requireContext(), newStatus)
            droidguardUnsupported.isVisible = newStatus && !DroidGuardPreferences.isAvailable(requireContext())
            true
        }
    }

    private fun runSafetyNetAttest() {
        val context = context ?: return
        runAttest.setIcon(R.drawable.ic_circle_pending)
        runAttest.setSummary(R.string.pref_test_summary_running)
        lifecycleScope.launchWhenResumed {
            try {
                val response = SafetyNet.getClient(requireActivity())
                    .attest(Random.nextBytes(32), SAFETYNET_API_KEY).await()
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
            } catch (e: Exception) {
                runAttest.summary = getString(R.string.pref_test_summary_failed, e.message)
                runAttest.icon = ContextCompat.getDrawable(context, R.drawable.ic_circle_warn)
            }
            updateContent()
        }
    }

    private fun runReCaptchaAttest() {
        val context = context ?: return
        runReCaptcha.setIcon(R.drawable.ic_circle_pending)
        runReCaptcha.setSummary(R.string.pref_test_summary_running)
        lifecycleScope.launchWhenResumed {
            try {
                val response = SafetyNet.getClient(requireActivity())
                    .verifyWithRecaptcha(RECAPTCHA_SITE_KEY).await()
                val result = if (response.tokenResult != null) {
                    val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
                    val json =
                        if (RECAPTCHA_SECRET != null) {
                            suspendCoroutine { continuation ->
                                queue.add(object : JsonObjectRequest(
                                    Method.POST,
                                    "https://www.google.com/recaptcha/api/siteverify",
                                    null,
                                    { continuation.resume(it) },
                                    { continuation.resumeWithException(it) }
                                ) {
                                    override fun getBodyContentType(): String = "application/x-www-form-urlencoded; charset=UTF-8"
                                    override fun getBody(): ByteArray =
                                        "secret=$RECAPTCHA_SECRET&response=${URLEncoder.encode(response.tokenResult, "UTF-8")}".encodeToByteArray()
                                })
                            }
                        } else {
                            // Can't properly verify, everything becomes a success
                            JSONObject(mapOf("success" to true))
                        }
                    Log.d(TAG, "Result: $json")
                    json.toString()
                } else {
                    null
                }
                formatSummaryForSafetyNetResult(context, result, response.result.status, RECAPTCHA)
                    .let { (summary, icon) ->
                        runReCaptcha.summary = summary
                        runReCaptcha.icon = icon
                    }
            } catch (e: Exception) {
                runReCaptcha.summary = getString(R.string.pref_test_summary_failed, e.message)
                runReCaptcha.icon = ContextCompat.getDrawable(context, R.drawable.ic_circle_warn)
            }
            updateContent()
        }
    }

    private fun runReCaptchaEnterpriseAttest() {
        val context = context ?: return
        runReCaptchaEnterprise.setIcon(R.drawable.ic_circle_pending)
        runReCaptchaEnterprise.setSummary(R.string.pref_test_summary_running)
        lifecycleScope.launchWhenResumed {
            try {
                val client = Recaptcha.getClient(requireActivity())
                val handle = client.init(RECAPTCHA_ENTERPRISE_SITE_KEY).await()
                val actionType = RecaptchaActionType.SIGNUP
                val response = client.execute(handle, RecaptchaAction(RecaptchaActionType(actionType))).await()
                Log.d(TAG, "Recaptcha Token: " + response.tokenResult)
                client.close(handle).await()
                val result = if (response.tokenResult != null) {
                    val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
                    val json = if (RECAPTCHA_ENTERPRISE_API_KEY != null) {
                        suspendCoroutine { continuation ->
                            queue.add(JsonObjectRequest(
                                Request.Method.POST,
                                "https://recaptchaenterprise.googleapis.com/v1/projects/$RECAPTCHA_ENTERPRISE_PROJECT_ID/assessments?key=$RECAPTCHA_ENTERPRISE_API_KEY",
                                JSONObject(
                                    mapOf(
                                        "event" to JSONObject(
                                            mapOf(
                                                "token" to response.tokenResult,
                                                "siteKey" to RECAPTCHA_ENTERPRISE_SITE_KEY,
                                                "expectedAction" to actionType
                                            )
                                        )
                                    )
                                ),
                                { continuation.resume(it) },
                                { continuation.resumeWithException(it) }
                            ))
                        }
                    } else {
                        // Can't properly verify, everything becomes a success
                        JSONObject(mapOf("tokenProperties" to JSONObject(mapOf("valid" to true)), "riskAnalysis" to JSONObject(mapOf("score" to "unknown"))))
                    }
                    Log.d(TAG, "Result: $json")
                    json.toString()
                } else {
                    null
                }
                formatSummaryForSafetyNetResult(context, result, null, RECAPTCHA_ENTERPRISE)
                    .let { (summary, icon) ->
                        runReCaptchaEnterprise.summary = summary
                        runReCaptchaEnterprise.icon = icon
                    }
            } catch (e: Exception) {
                runReCaptchaEnterprise.summary = getString(R.string.pref_test_summary_failed, e.message)
                runReCaptchaEnterprise.icon = ContextCompat.getDrawable(context, R.drawable.ic_circle_warn)
            }
            updateContent()
        }
    }

    override fun onResume() {
        super.onResume()

        switchBarPreference.isEnabled = CheckinPreferences.isEnabled(requireContext())
        switchBarPreference.isChecked = SafetyNetPreferences.isEnabled(requireContext()) && DroidGuardPreferences.isEnabled(requireContext())
        droidguardUnsupported.isVisible = switchBarPreference.isChecked && !DroidGuardPreferences.isAvailable(requireContext())

        updateContent()
    }

    fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            val (apps, showAll) = withContext(Dispatchers.IO) {
                val playIntegrityData = VendingPreferences.getPlayIntegrityAppList(context)
                val db = SafetyNetDatabase(context)
                val apps = try {
                    db.recentApps + PlayIntegrityData.loadDataSet(playIntegrityData).map { it.packageName to it.lastTime }
                } finally {
                    db.close()
                }
                apps.sortedByDescending { it.second }.map { app ->
                    app to context.packageManager.getApplicationInfoIfExists(app.first)
                }.mapNotNull { (app, info) ->
                    if (info == null) null else app to info
                }.take(3).mapIndexed { idx, (app, applicationInfo) ->
                    val pref = AppIconPreference(context)
                    pref.order = idx
                    pref.applicationInfo = applicationInfo
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
            this@SafetyNetFragment.apps.removeAll()
            for (app in apps) {
                this@SafetyNetFragment.apps.addPreference(app)
            }
            if (showAll) {
                this@SafetyNetFragment.apps.addPreference(appsAll)
            } else if (apps.isEmpty()) {
                this@SafetyNetFragment.apps.addPreference(appsNone)
            }

        }
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_ADVANCED, 0, org.microg.gms.base.core.R.string.menu_advanced)
        menu.add(0, MENU_CLEAR_REQUESTS, 0, R.string.menu_clear_recent_requests)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ADVANCED -> {
                findNavController().navigate(requireContext(), R.id.openSafetyNetAdvancedSettings)
                true
            }
            MENU_CLEAR_REQUESTS -> {
                val db = SafetyNetDatabase(requireContext())
                db.clearAllRequests()
                db.close()
                updateContent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private val SAFETYNET_API_KEY: String? = BuildConfig.SAFETYNET_KEY.takeIf { it.isNotBlank() }
        private val RECAPTCHA_SITE_KEY: String? = BuildConfig.RECAPTCHA_SITE_KEY.takeIf { it.isNotBlank() }
        private val RECAPTCHA_SECRET: String? = BuildConfig.RECAPTCHA_SECRET.takeIf { it.isNotBlank() }
        private val RECAPTCHA_ENTERPRISE_PROJECT_ID: String? = BuildConfig.RECAPTCHA_ENTERPRISE_PROJECT_ID.takeIf { it.isNotBlank() }
        private val RECAPTCHA_ENTERPRISE_SITE_KEY: String? = BuildConfig.RECAPTCHA_ENTERPRISE_SITE_KEY.takeIf { it.isNotBlank() }
        private val RECAPTCHA_ENTERPRISE_API_KEY: String? = BuildConfig.RECAPTCHA_ENTERPRISE_API_KEY.takeIf { it.isNotBlank() }
        private const val MENU_ADVANCED = Menu.FIRST
        private const val MENU_CLEAR_REQUESTS = Menu.FIRST + 1
    }
}
