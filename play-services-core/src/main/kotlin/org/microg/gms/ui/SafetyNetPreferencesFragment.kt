/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.R
import com.google.android.gms.recaptcha.Recaptcha
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaActionType
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.safetynet.SafetyNetDatabase
import org.microg.gms.safetynet.SafetyNetRequestType.ATTESTATION
import org.microg.gms.safetynet.SafetyNetRequestType.RECAPTCHA
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class SafetyNetPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var runAttest: Preference
    private lateinit var runReCaptcha: Preference
    private lateinit var runReCaptchaEnterprise: Preference
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
        runReCaptchaEnterprise =
            preferenceScreen.findPreference("pref_recaptcha_enterprise_run_test") ?: runReCaptchaEnterprise
        apps = preferenceScreen.findPreference("prefcat_safetynet_apps") ?: apps
        appsAll = preferenceScreen.findPreference("pref_safetynet_apps_all") ?: appsAll
        appsNone = preferenceScreen.findPreference("pref_safetynet_apps_none") ?: appsNone

        runAttest.isVisible = SAFETYNET_API_KEY != null
        runReCaptcha.isVisible = RECAPTCHA_SITE_KEY != null
        runReCaptchaEnterprise.isVisible = RECAPTCHA_ENTERPRISE_SITE_KEY != null

        runAttest.setOnPreferenceClickListener {
            val context = context ?: return@setOnPreferenceClickListener false
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
            true
        }
        runReCaptcha.setOnPreferenceClickListener {
            val context = context ?: return@setOnPreferenceClickListener false
            runReCaptcha.setIcon(R.drawable.ic_circle_pending)
            runReCaptcha.setSummary(R.string.pref_test_summary_running)
            lifecycleScope.launchWhenResumed {
                try {
                    val response = SafetyNet.getClient(requireActivity())
                        .verifyWithRecaptcha(RECAPTCHA_SITE_KEY).await()
                    val result = if (response.tokenResult != null) {
                        val queue = Volley.newRequestQueue(context)
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
            true
        }
        runReCaptchaEnterprise.setOnPreferenceClickListener {
            val context = context ?: return@setOnPreferenceClickListener false
            runReCaptchaEnterprise.setIcon(R.drawable.ic_circle_pending)
            runReCaptchaEnterprise.setSummary(R.string.pref_test_summary_running)
            lifecycleScope.launchWhenResumed {
                try {
                    val client = Recaptcha.getClient(requireActivity())
                    val handle = client.init(RECAPTCHA_ENTERPRISE_SITE_KEY).await()
                    val result =
                        client.execute(handle, RecaptchaAction(RecaptchaActionType(RecaptchaActionType.SIGNUP))).await()
                    Log.d(TAG, "Recaptcha Token: " + result.tokenResult)
                    client.close(handle).await()
                    runReCaptchaEnterprise.summary = getString(R.string.pref_test_summary_warn, "Incomplete Test")
                    runReCaptchaEnterprise.icon = ContextCompat.getDrawable(context, R.drawable.ic_circle_warn)
                } catch (e: Exception) {
                    runReCaptchaEnterprise.summary = getString(R.string.pref_test_summary_failed, e.message)
                    runReCaptchaEnterprise.icon = ContextCompat.getDrawable(context, R.drawable.ic_circle_warn)
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
                val db = SafetyNetDatabase(context)
                val apps = try {
                    db.recentApps
                } finally {
                    db.close()
                }
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

    companion object {
        private val SAFETYNET_API_KEY: String? = "AIzaSyCcJO6IZiA5Or_AXw3LFdaTCmpnfL4pJ-Q"
        private val RECAPTCHA_SITE_KEY: String? = "6Lc4TzgeAAAAAJnW7Jbo6UtQ0xGuTKjHAeyhINuq"
        private val RECAPTCHA_SECRET: String? = "6Lc4TzgeAAA${"AAAjwSDqU-uG"}_Lcu2f23URMI8fq0I"
        private val RECAPTCHA_ENTERPRISE_SITE_KEY: String? = null
    }
}
