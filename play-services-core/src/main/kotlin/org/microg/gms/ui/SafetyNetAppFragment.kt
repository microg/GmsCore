/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.google.android.gms.R
import org.microg.gms.safetynet.SafetyNetDatabase
import org.microg.gms.safetynet.SafetyNetRequestType.*

class SafetyNetAppFragment : PreferenceFragmentCompat() {
    private lateinit var appHeadingPreference: AppHeadingPreference
    private lateinit var recents: PreferenceCategory
    private lateinit var recentsNone: Preference
    private val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_safetynet_app)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        appHeadingPreference = preferenceScreen.findPreference("pref_safetynet_app_heading") ?: appHeadingPreference
        recents = preferenceScreen.findPreference("prefcat_safetynet_recent_list") ?: recents
        recentsNone = preferenceScreen.findPreference("pref_safetynet_recent_none") ?: recentsNone
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    fun updateContent() {
        lifecycleScope.launchWhenResumed {
            appHeadingPreference.packageName = packageName
            val context = requireContext()
            val summaries =
                packageName?.let { packageName ->
                    val db = SafetyNetDatabase(context)
                    try {
                        db.getRecentRequests(packageName)
                    } finally {
                        db.close()
                    }
                }.orEmpty()
            recents.removeAll()
            recents.addPreference(recentsNone)
            recentsNone.isVisible = summaries.isEmpty()
            for (summary in summaries) {
                val preference = Preference(requireContext())
                preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    SafetyNetRecentDialogFragment().apply {
                        arguments = Bundle().apply { putParcelable("summary", summary) }
                    }.show(requireFragmentManager(), null)
                    true
                }
                val date = DateUtils.getRelativeDateTimeString(
                    context,
                    summary.timestamp,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_TIME
                )
                preference.title = date
                formatSummaryForSafetyNetResult(
                    context,
                    summary.responseData,
                    summary.responseStatus,
                    summary.requestType
                ).let { (text, icon) ->
                    preference.summary = when (summary.requestType) {
                        ATTESTATION -> getString(R.string.pref_safetynet_recent_attestation_summary, text)
                        RECAPTCHA -> getString(R.string.pref_safetynet_recent_recaptcha_summary, text)
                        RECAPTCHA_ENTERPRISE -> getString(R.string.pref_safetynet_recent_recaptcha_enterprise_summary, text)
                    }
                    preference.icon = icon
                }
                recents.addPreference(preference)
            }
        }

    }
}
