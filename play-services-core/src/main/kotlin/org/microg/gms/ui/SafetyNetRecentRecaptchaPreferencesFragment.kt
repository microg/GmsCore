package org.microg.gms.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import org.microg.gms.safetynet.SafetyNetSummary


class SafetyNetRecentRecaptchaPreferencesFragment : PreferenceFragmentCompat() {

    lateinit var snetSummary: SafetyNetSummary

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_snet_recent_recaptcha)
        snetSummary = arguments?.get("summary") as SafetyNetSummary
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        val requestType: Preference = preferenceScreen.findPreference("pref_request_type")!!
        val time : Preference = preferenceScreen.findPreference("pref_time")!!
        val status : Preference = preferenceScreen.findPreference("pref_status")!!
        val token : Preference = preferenceScreen.findPreference("pref_token")!!

        requestType.summary = "RECAPTCHA"

        time.summary = DateUtils.getRelativeDateTimeString(context, snetSummary.timestamp, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME)


        if(snetSummary.responseStatus==null){
            status.summary = "Not completed yet"
        }else{
            status.summary = snetSummary.responseStatus!!.statusMessage
            token.summary = snetSummary.responseData
        }

    }

}