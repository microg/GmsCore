package org.microg.gms.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.google.android.gms.R
import com.google.android.gms.databinding.SafetyNetRecentFragmentBinding
import org.microg.gms.safetynet.SafetyNetRequestType
import org.microg.gms.safetynet.SafetyNetSummary

class SafetyNetRecentFragment : Fragment(R.layout.safety_net_recent_fragment) {


    class MyListView(context: Context) : ListView(context) {

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SafetyNetRecentFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val summary = arguments!!.get("summary") as SafetyNetSummary

        if(savedInstanceState==null){
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                if(summary.requestType==SafetyNetRequestType.ATTESTATION){
                    add<SafetyNetRecentAttestationPreferencesFragment>(R.id.sub_preferences, args=arguments)
                }else{
                    add<SafetyNetRecentRecaptchaPreferencesFragment>(R.id.sub_preferences, args=arguments)
                }
            }
        }



        val pm = context!!.packageManager
        val appInfo = pm.getApplicationInfoIfExists(summary.packageName)
        if(appInfo==null) return Toast.makeText(context, "Application not installed", Toast.LENGTH_SHORT).show()


        binding.appIcon.setImageDrawable(appInfo.loadIcon(pm))
        binding.appName.text = appInfo.loadLabel(pm)
        binding.packageName.text = summary.packageName

    }

    lateinit var binding: SafetyNetRecentFragmentBinding

}
