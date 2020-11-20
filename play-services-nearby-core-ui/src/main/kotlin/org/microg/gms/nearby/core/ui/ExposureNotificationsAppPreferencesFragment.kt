/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import org.json.JSONObject
import org.microg.gms.nearby.exposurenotification.ExposureDatabase
import org.microg.gms.nearby.exposurenotification.merge

class ExposureNotificationsAppPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var open: Preference
    private lateinit var report: Preference
    private lateinit var apiUsage: Preference
    private val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications_app)
    }

    override fun onBindPreferences() {
        open = preferenceScreen.findPreference("pref_exposure_app_open") ?: open
        report = preferenceScreen.findPreference("pref_exposure_app_report") ?: report
        apiUsage = preferenceScreen.findPreference("pref_exposure_app_api_usage") ?: apiUsage
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

    private fun ExposureConfiguration?.orDefault() = this
            ?: ExposureConfiguration.ExposureConfigurationBuilder().build()

    fun updateContent() {
        packageName?.let { packageName ->
            lifecycleScope.launchWhenResumed {
                val (reportTitle, reportSummary, apiUsageSummary) = ExposureDatabase.with(requireContext()) { database ->
                    val apiUsageSummary = database.methodUsageHistogram(packageName).map {
                        getString(R.string.pref_exposure_app_api_usage_summary_line, it.second, it.first.let { "<tt>$it</tt>" })
                    }.joinToString("<br>").takeIf { it.isNotEmpty() }
                    val token = database.lastMethodCallArgs(packageName, "provideDiagnosisKeys")?.let { JSONObject(it).getString("request_token") }
                            ?: return@with Triple(null, null, apiUsageSummary)
                    val lastCheckTime = database.lastMethodCall(packageName, "provideDiagnosisKeys")
                            ?: return@with Triple(null, null, apiUsageSummary)
                    val config = database.loadConfiguration(packageName, token)
                            ?: return@with Triple(null, null, apiUsageSummary)
                    val merged = database.findAllMeasuredExposures(config.first).merge().sortedBy { it.timestamp }
                    val reportTitle = getString(R.string.pref_exposure_app_last_report_title, DateUtils.getRelativeTimeSpanString(lastCheckTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS))
                    val diagnosisKeysLine = getString(R.string.pref_exposure_app_last_report_summary_diagnosis_keys, database.countDiagnosisKeysInvolved(config.first))
                    val encountersLine = if (merged.isEmpty()) {
                        getString(R.string.pref_exposure_app_last_report_summary_encounters_no)
                    } else {
                        merged.map {
                            val riskScore = it.getRiskScore(config.second.orDefault())
                            "· " + getString(R.string.pref_exposure_app_last_report_summary_encounters_line, DateUtils.formatDateRange(requireContext(), it.timestamp, it.timestamp + it.durationInMinutes * 60 * 1000L, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE), riskScore)
                        }.joinToString("<br>").let { getString(R.string.pref_exposure_app_last_report_summary_encounters_prefix, merged.size) + "<br>$it<br><i>" + getString(R.string.pref_exposure_app_last_report_summary_encounters_suffix) + "</i>" }
                    }
                    Triple(reportTitle, "$diagnosisKeysLine<br>$encountersLine", apiUsageSummary)
                }
                report.isVisible = reportSummary != null
                report.title = reportTitle
                report.summary = HtmlCompat.fromHtml(reportSummary.orEmpty(), HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
                apiUsage.isVisible = apiUsageSummary != null
                apiUsage.summary = HtmlCompat.fromHtml(apiUsageSummary.orEmpty(), HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
            }
        }
    }
}
