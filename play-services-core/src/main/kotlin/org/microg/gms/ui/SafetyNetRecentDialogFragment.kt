package org.microg.gms.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.google.android.gms.R
import com.google.android.gms.databinding.SafetyNetRecentFragmentBinding
import org.microg.gms.safetynet.SafetyNetRequestType
import org.microg.gms.safetynet.SafetyNetRequestType.*
import org.microg.gms.safetynet.SafetyNetSummary

class SafetyNetRecentDialogFragment : DialogFragment(R.layout.safety_net_recent_fragment) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SafetyNetRecentFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val summary = arguments?.get("summary") as SafetyNetSummary

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                setReorderingAllowed(true)
                when (summary.requestType) {
                    ATTESTATION -> add<SafetyNetRecentAttestationPreferencesFragment>(
                        R.id.actual_content,
                        args = arguments
                    )
                    RECAPTCHA -> add<SafetyNetRecentRecaptchaPreferencesFragment>(
                        R.id.actual_content,
                        args = arguments
                    )
                    RECAPTCHA_ENTERPRISE -> add<SafetyNetRecentRecaptchaPreferencesFragment>(
                        R.id.actual_content,
                        args = arguments
                    )
                }
            }
        }

        dialog?.window?.apply {
            attributes = attributes.apply {
                width = LayoutParams.MATCH_PARENT
            }
        }
    }

    lateinit var binding: SafetyNetRecentFragmentBinding

}
