/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import org.json.JSONObject
import org.microg.gms.nearby.exposurenotification.ExposureDatabase
import org.microg.gms.nearby.exposurenotification.merge

class ExposureNotificationsAppPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var open: Preference
    private lateinit var reportedExposures: PreferenceCategory
    private lateinit var reportedExposuresNone: Preference
    private lateinit var reportedExposuresUpdated: Preference
    private lateinit var apiUsage: Preference
    private val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications_app)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        open = preferenceScreen.findPreference("pref_exposure_app_open") ?: open
        reportedExposures = preferenceScreen.findPreference("prefcat_exposure_app_report") ?: reportedExposures
        reportedExposuresNone = preferenceScreen.findPreference("pref_exposure_app_report_none")
                ?: reportedExposuresNone
        reportedExposuresUpdated = preferenceScreen.findPreference("pref_exposure_app_report_updated")
                ?: reportedExposuresUpdated
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

    private fun formatRelativeDateTimeString(time: Long): CharSequence? =
            DateUtils.getRelativeDateTimeString(
                    requireContext(),
                    time,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS * 2,
                    0
            )

    fun updateContent() {
        packageName?.let { packageName ->
            lifecycleScope.launchWhenResumed {
                data class NTuple4<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)
                val (mergedExposures, keysInvolved, lastCheckTime, methodUsageHistogram) = ExposureDatabase.with(requireContext()) { database ->
                    val methodUsageHistogram = database.methodUsageHistogram(packageName)

                    val token = database.lastMethodCallArgs(packageName, "provideDiagnosisKeys")?.let { JSONObject(it).getString("request_token") }
                            ?: return@with NTuple4(null, null, null, methodUsageHistogram)
                    val lastCheckTime = database.lastMethodCall(packageName, "provideDiagnosisKeys")
                            ?: return@with NTuple4(null, null, null, methodUsageHistogram)
                    val config = database.loadConfiguration(packageName, token)
                            ?: return@with NTuple4(null, null, null, methodUsageHistogram)
                    val mergedExposures = database.findAllMeasuredExposures(config.first).merge().sortedBy { it.timestamp }
                    val keysInvolved = database.countDiagnosisKeysInvolved(config.first)
                    NTuple4(mergedExposures, keysInvolved, lastCheckTime, methodUsageHistogram)
                }

                reportedExposures.removeAll()
                reportedExposures.addPreference(reportedExposuresNone)
                if (mergedExposures.isNullOrEmpty()) {
                    reportedExposuresNone.isVisible = true
                } else {
                    reportedExposuresNone.isVisible = false
                    for (exposure in mergedExposures) {
                        val minAttenuation = exposure.subs.map { it.attenuation }.minOrNull() ?: exposure.attenuation
                        val nearby = exposure.attenuation < 63 || minAttenuation < 55
                        val distanceString = if (nearby) getString(R.string.pref_exposure_app_report_entry_distance_close) else getString(R.string.pref_exposure_app_report_entry_distance_far)
                        val durationString = if (exposure.durationInMinutes < 5) getString(R.string.pref_exposure_app_report_entry_time_short) else getString(R.string.pref_exposure_app_report_entry_time_about, exposure.durationInMinutes)
                        val preference = object : Preference(requireContext()) {
                            override fun onBindViewHolder(holder: PreferenceViewHolder) {
                                val titleView = holder.findViewById(android.R.id.title) as? TextView
                                val titleViewTextColor = titleView?.textColors
                                super.onBindViewHolder(holder)
                                if (titleViewTextColor != null) titleView.setTextColor(titleViewTextColor)
                            }
                        }
                        preference.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_alert)
                        preference.title = DateUtils.formatDateRange(requireContext(), exposure.timestamp, exposure.timestamp + exposure.durationInMinutes * 60 * 1000L, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
                        preference.summary = getString(R.string.pref_exposure_app_report_entry_combined, durationString, distanceString)
                        preference.isSelectable = false
                        reportedExposures.addPreference(preference)
                    }
                }

                reportedExposuresUpdated.isVisible = lastCheckTime != null
                reportedExposuresUpdated.title = if (lastCheckTime != null) getString(R.string.pref_exposure_app_report_updated_title, DateUtils.getRelativeDateTimeString(requireContext(), lastCheckTime, DateUtils.DAY_IN_MILLIS, DateUtils.DAY_IN_MILLIS * 2, 0)) else null
                reportedExposuresUpdated.summary = getString(R.string.pref_exposure_app_last_report_summary_diagnosis_keys, keysInvolved?.toInt()
                        ?: 0)
                reportedExposures.addPreference(reportedExposuresUpdated)

                val apiUsageSummary = methodUsageHistogram.map {
                    getString(R.string.pref_exposure_app_api_usage_summary_line, it.second, it.first.let { "<small><tt>$it</tt></small>" })
                }.joinToString("<br>").takeIf { it.isNotEmpty() }
                apiUsage.isVisible = apiUsageSummary != null
                apiUsage.summary = HtmlCompat.fromHtml(apiUsageSummary.orEmpty(), HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
            }
        }
    }
}
