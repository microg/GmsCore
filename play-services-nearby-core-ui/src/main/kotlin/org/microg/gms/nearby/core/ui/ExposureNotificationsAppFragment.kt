/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import org.microg.gms.nearby.core.ui.databinding.ExposureNotificationsAppFragmentBinding
import org.microg.gms.ui.getApplicationInfoIfExists

class ExposureNotificationsAppFragment : Fragment(R.layout.exposure_notifications_app_fragment) {
    private lateinit var binding: ExposureNotificationsAppFragmentBinding
    val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ExposureNotificationsAppFragmentBinding.inflate(inflater, container, false)
        binding.callbacks = object : ExposureNotificationsAppFragmentCallbacks {
            override fun onAppClicked() {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                context!!.startActivity(intent)
            }
        }
        childFragmentManager.findFragmentById(R.id.sub_preferences)?.arguments = arguments
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            val pm = appContext.packageManager
            val applicationInfo = pm.getApplicationInfoIfExists(packageName)
            binding.appName = applicationInfo?.loadLabel(pm)?.toString() ?: packageName
            binding.appIcon = applicationInfo?.loadIcon(pm)
                    ?: AppCompatResources.getDrawable(appContext, android.R.mipmap.sym_def_app_icon)
        }
    }
}

interface ExposureNotificationsAppFragmentCallbacks {
    fun onAppClicked()
}
