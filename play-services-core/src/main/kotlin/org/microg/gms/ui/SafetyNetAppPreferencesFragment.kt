/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.PushRegisterManager
import org.microg.gms.safetynet.SafetyNetDatabase
import org.microg.gms.safetynet.SafetyNetRequestType
import org.microg.gms.safetynet.SafetyNetRequestType.ATTESTATION
import org.microg.gms.safetynet.SafetyNetRequestType.RECAPTCHA

class SafetyNetAppPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var recents: PreferenceCategory
    private lateinit var recentsNone: Preference
    private val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_safetynet_app)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        recents = preferenceScreen.findPreference("prefcat_safetynet_recent_list") ?: recents
        recentsNone = preferenceScreen.findPreference("pref_safetynet_recent_none") ?: recentsNone
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            val summaries =
                packageName?.let { packageName -> SafetyNetDatabase(context).use { it.getRecentRequests(packageName) } }
                    .orEmpty()
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
                        ATTESTATION -> "Attestation: $text"
                        RECAPTCHA -> "ReCaptcha: $text"
                    }
                    preference.icon = icon
                }
                recents.addPreference(preference)
            }
        }

    }
}
