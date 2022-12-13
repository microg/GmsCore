package org.microg.gms.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.firebase.auth.getStringOrNull
import org.microg.gms.safetynet.SafetyNetSummary
import org.microg.gms.utils.toHexString


class SafetyNetRecentAttestationPreferencesFragment : PreferenceFragmentCompat() {

    lateinit var snetSummary: SafetyNetSummary

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_snet_recent_attestation)
        snetSummary = arguments?.get("summary") as SafetyNetSummary
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        val requestType: Preference = preferenceScreen.findPreference("pref_request_type")!!
        val time : Preference = preferenceScreen.findPreference("pref_time")!!
        val nonce : Preference = preferenceScreen.findPreference("pref_nonce")!!
        val status : Preference = preferenceScreen.findPreference("pref_status")!!
        val evalType : Preference = preferenceScreen.findPreference("pref_eval_type")!!
        val advice : Preference = preferenceScreen.findPreference("pref_advice")!!
        val copyResult : Preference = preferenceScreen.findPreference("pref_copy_result")!!

        requestType.summary = "ATTESTATION"

        time.summary = DateUtils.getRelativeDateTimeString(context, snetSummary.timestamp, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME)


        snetSummary.nonce?.toHexString().let {
            if (it == null) {
                nonce.summary = "None"
            } else {
                nonce.summary = it
            }
        }

        if(snetSummary.responseStatus==null){
            status.summary = "Not completed yet"
        }else if(snetSummary.responseStatus!!.isSuccess) {
            try {
                val json = JSONObject(snetSummary.responseData!!)
                evalType.summary = json.getString("evaluationType")
                advice.summary = json.getStringOrNull("advice") ?: "None"

                val basicIntegrity = json.getBoolean("basicIntegrity")
                val ctsProfileMatch = json.getBoolean("ctsProfileMatch")

                status.summary = when{
                    basicIntegrity && ctsProfileMatch -> {
                        "Integrity and CTS passed"
                    }
                    basicIntegrity -> {
                        "CTS failed"
                    }
                    else -> {
                        "Integrity failed"
                    }
                }

            } catch (e: JSONException) {
                e.printStackTrace()
                status.summary = "Invalid JSON"
            }
        }else{
            status.summary = snetSummary.responseStatus!!.statusMessage
        }

        copyResult.setOnPreferenceClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("JSON JWS data", snetSummary.responseData)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(context, "Copied to clipboard !", Toast.LENGTH_SHORT).show()

            true
        }

    }

}
