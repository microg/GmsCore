/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.databinding.ExposureNotificationsFragmentBinding
import org.microg.gms.nearby.exposurenotification.ServiceInfo
import org.microg.gms.nearby.exposurenotification.getExposureNotificationsServiceInfo
import org.microg.gms.nearby.exposurenotification.setExposureNotificationsServiceConfiguration

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
        lifecycleScope.launchWhenResumed {
            val info = getExposureNotificationsServiceInfo(requireContext())
            val newConfiguration = info.configuration.copy(enabled = newStatus)
            displayServiceInfo(setExposureNotificationsServiceConfiguration(requireContext(), newConfiguration))
        }
    }

    fun displayServiceInfo(serviceInfo: ServiceInfo) {
        binding.scannerEnabled = serviceInfo.configuration.enabled
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            displayServiceInfo(getExposureNotificationsServiceInfo(requireContext()))
        }
    }
}
