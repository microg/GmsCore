/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import org.microg.gms.nearby.core.R
import org.microg.gms.nearby.core.databinding.ExposureNotificationsFragmentBinding
import org.microg.gms.nearby.exposurenotification.ServiceInfo
import org.microg.gms.nearby.exposurenotification.getExposureNotificationsServiceInfo
import org.microg.gms.nearby.exposurenotification.setExposureNotificationsServiceConfiguration
import org.microg.gms.ui.PreferenceSwitchBarCallback

class ExposureNotificationsFragment : Fragment(R.layout.exposure_notifications_fragment) {
    private lateinit var binding: ExposureNotificationsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ExposureNotificationsFragmentBinding.inflate(inflater, container, false)
        binding.switchBarCallback = object : PreferenceSwitchBarCallback {
            override fun onChecked(newStatus: Boolean) {
                setEnabled(newStatus)
            }
        }
        return binding.root
    }

    fun setEnabled(newStatus: Boolean) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            val info = getExposureNotificationsServiceInfo(appContext)
            val newConfiguration = info.configuration.copy(enabled = newStatus)
            setExposureNotificationsServiceConfiguration(appContext, newConfiguration)
            displayServiceInfo(info.copy(configuration = newConfiguration))
        }
    }

    fun displayServiceInfo(serviceInfo: ServiceInfo) {
        binding.scannerEnabled = serviceInfo.configuration.enabled
    }

    override fun onResume() {
        super.onResume()
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            displayServiceInfo(getExposureNotificationsServiceInfo(appContext))
        }
    }
}
