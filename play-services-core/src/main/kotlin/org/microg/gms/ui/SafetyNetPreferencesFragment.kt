/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import com.google.android.gms.common.api.Status
import com.google.android.gms.safetynet.AttestationData
import com.google.android.gms.safetynet.RecaptchaResultData
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.safetynet.SafetyNetClientService
import org.microg.gms.safetynet.SafetyNetClientServiceImpl
import kotlin.random.Random

class SafetyNetPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var runAttest: Preference
    private lateinit var runReCaptcha: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_safetynet)
    }

    override fun onBindPreferences() {
        runAttest = preferenceScreen.findPreference("pref_snet_run_attest") ?: runAttest
        runReCaptcha = preferenceScreen.findPreference("pref_recaptcha_run_test") ?: runReCaptcha

        // TODO: Use SafetyNet client library once ready
        runAttest.setOnPreferenceClickListener {
            val context = context ?: return@setOnPreferenceClickListener false
            runAttest.setIcon(R.drawable.ic_circle_pending)
            runAttest.setSummary(R.string.pref_test_summary_running)
            val handler = Handler(Looper.myLooper()!!)
            SafetyNetClientServiceImpl(context, context.packageName, lifecycle).attestWithApiKey(object : ISafetyNetCallbacks.Default() {
                override fun onAttestationData(status: Status?, attestationData: AttestationData?) {
                    handler.post {
                        if (status?.isSuccess == true) {
                            if (attestationData?.jwsResult == null) {
                                runAttest.setIcon(R.drawable.ic_circle_warn)
                                runAttest.summary = context.getString(R.string.pref_test_summary_failed, "No result")
                            } else {
                                val (_, payload, _) = try {
                                    attestationData.jwsResult.split(".")
                                } catch (e: Exception) {
                                    runAttest.setIcon(R.drawable.ic_circle_error)
                                    runAttest.summary = context.getString(R.string.pref_test_summary_failed, "Invalid JWS")
                                    return@post
                                }
                                val (basicIntegrity, ctsProfileMatch, advice) = try {
                                    JSONObject(Base64.decode(payload, Base64.URL_SAFE).decodeToString()).let {
                                        Triple(it.optBoolean("basicIntegrity", false), it.optBoolean("ctsProfileMatch", false), it.optString("advice", ""))
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, e)
                                    runAttest.setIcon(R.drawable.ic_circle_error)
                                    runAttest.summary = context.getString(R.string.pref_test_summary_failed, "Invalid JSON")
                                    return@post
                                }
                                val adviceText = if (advice == "") "" else "\n" + advice.split(",").map {
                                    when (it) {
                                        "LOCK_BOOTLOADER" -> "Bootloader is not locked"
                                        "RESTORE_TO_FACTORY_ROM" -> "ROM is not clean"
                                        else -> it
                                    }
                                }.joinToString("\n")
                                when {
                                    basicIntegrity && ctsProfileMatch -> {
                                        runAttest.setIcon(R.drawable.ic_circle_check)
                                        runAttest.setSummary(R.string.pref_test_summary_passed)
                                    }
                                    basicIntegrity -> {
                                        runAttest.setIcon(R.drawable.ic_circle_warn)
                                        runAttest.summary = context.getString(R.string.pref_test_summary_warn, "CTS profile does not match$adviceText")
                                    }
                                    else -> {
                                        runAttest.setIcon(R.drawable.ic_circle_error)
                                        runAttest.summary = context.getString(R.string.pref_test_summary_failed, "integrity check failed$adviceText")
                                    }
                                }
                            }
                        } else {
                            runAttest.setIcon(R.drawable.ic_circle_error)
                            runAttest.summary = context.getString(R.string.pref_test_summary_failed, status?.statusMessage)
                        }
                    }
                }
            }, Random.nextBytes(32), "AIzaSyCcJO6IZiA5Or_AXw3LFdaTCmpnfL4pJ-Q")
            true
        }
        runReCaptcha.setOnPreferenceClickListener {
            val context = context ?: return@setOnPreferenceClickListener false
            runReCaptcha.setIcon(R.drawable.ic_circle_pending)
            runReCaptcha.setSummary(R.string.pref_test_summary_running)
            val handler = Handler(Looper.myLooper()!!)
            SafetyNetClientServiceImpl(context, context.packageName, lifecycle).verifyWithRecaptcha(object : ISafetyNetCallbacks.Default() {
                override fun onRecaptchaResult(status: Status?, recaptchaResultData: RecaptchaResultData?) {
                    handler.post {
                        if (status?.isSuccess == true) {
                            runReCaptcha.setIcon(R.drawable.ic_circle_check)
                            runReCaptcha.setSummary(R.string.pref_test_summary_passed)
                        } else {
                            runReCaptcha.setIcon(R.drawable.ic_circle_error)
                            runReCaptcha.summary = context.getString(R.string.pref_test_summary_failed, status?.statusMessage)
                        }
                    }
                }
            }, "6Lc4TzgeAAAAAJnW7Jbo6UtQ0xGuTKjHAeyhINuq")
            true
        }
    }
}
