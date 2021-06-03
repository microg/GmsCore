/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.R
import com.google.android.gms.databinding.PushNotificationFragmentBinding
import org.microg.gms.checkin.getCheckinServiceInfo
import org.microg.gms.gcm.ServiceInfo
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.gms.gcm.setGcmServiceConfiguration

class PushNotificationFragment : Fragment(R.layout.push_notification_fragment) {
    lateinit var binding: PushNotificationFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PushNotificationFragmentBinding.inflate(inflater, container, false)
        binding.switchBarCallback = object : PreferenceSwitchBarCallback {
            override fun onChecked(newStatus: Boolean) {
                setEnabled(newStatus)
            }
        }
        return binding.root
    }

    fun setEnabled(newStatus: Boolean) {
        lifecycleScope.launchWhenResumed {
            val info = getGcmServiceInfo(requireContext())
            val newConfiguration = info.configuration.copy(enabled = newStatus)
            setGcmServiceConfiguration(requireContext(), newConfiguration)
            displayServiceInfo(info.copy(configuration = newConfiguration))
        }
    }

    private fun displayServiceInfo(serviceInfo: ServiceInfo) {
        binding.gcmEnabled = serviceInfo.configuration.enabled
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            displayServiceInfo(getGcmServiceInfo(requireContext()))
            binding.checkinEnabled = getCheckinServiceInfo(requireContext()).configuration.enabled
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_ADVANCED, 0, R.string.menu_advanced)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ADVANCED -> {
                findNavController().navigate(requireContext(), R.id.openGcmAdvancedSettings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_ADVANCED = Menu.FIRST
    }
}

