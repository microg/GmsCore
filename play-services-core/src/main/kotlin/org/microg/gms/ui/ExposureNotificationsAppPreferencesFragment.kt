/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.util.JsonReader
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import org.json.JSONObject
import org.microg.gms.nearby.exposurenotification.ExposureDatabase
import java.util.concurrent.TimeUnit

class ExposureNotificationsAppPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var open: Preference
    private lateinit var checks: Preference
    private val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications_app)
    }

    override fun onBindPreferences() {
        open = preferenceScreen.findPreference("pref_exposure_app_open") ?: open
        checks = preferenceScreen.findPreference("pref_exposure_app_checks") ?: checks
        open.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                packageName?.let {
                    context?.packageManager?.getLaunchIntentForPackage(it)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context?.startActivity(intent)
                    }
                }
            } catch (ignored: Exception) {
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    fun updateContent() {
        packageName?.let { packageName ->
            ExposureDatabase.with(requireContext()) { database ->
                var str = getString(R.string.pref_exposure_app_checks_summary, database.countMethodCalls(packageName, "provideDiagnosisKeys"))
                val lastCheckTime = database.lastMethodCall(packageName, "provideDiagnosisKeys")
                if (lastCheckTime != null && lastCheckTime != 0L) {
                    str += "\n" + getString(R.string.pref_exposure_app_last_check_summary, DateUtils.getRelativeDateTimeString(context, lastCheckTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME))
                }
                val lastExposureSummaryTime = database.lastMethodCall(packageName, "getExposureSummary")
                val lastExposureSummary = database.lastMethodCallArgs(packageName, "getExposureSummary")
                if (lastExposureSummaryTime != null && lastExposureSummary != null && System.currentTimeMillis() - lastExposureSummaryTime <= TimeUnit.DAYS.toMillis(1)) {
                    try {
                        val json = JSONObject(lastExposureSummary)
                        val matchedKeys = json.optInt("response_matched_keys")
                        val daysSince = json.optInt("response_days_since", -1)
                        if (matchedKeys > 0 && daysSince >= 0) {
                            str += "\n" + resources.getQuantityString(R.plurals.pref_exposure_app_last_report_summary, matchedKeys, matchedKeys, daysSince)
                        }
                    } catch (ignored: Exception) {
                    }
                }
                checks.summary = str
            }
        }
    }
}
