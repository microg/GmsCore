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
import com.google.android.gms.databinding.SafetyNetFragmentBinding
import org.microg.gms.checkin.getCheckinServiceInfo
import org.microg.gms.safetynet.ServiceInfo
import org.microg.gms.safetynet.getSafetyNetServiceInfo
import org.microg.gms.safetynet.setSafetyNetServiceConfiguration

class SafetyNetFragment : Fragment(R.layout.safety_net_fragment) {

    private lateinit var binding: SafetyNetFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SafetyNetFragmentBinding.inflate(inflater, container, false)
        binding.switchBarCallback = object : PreferenceSwitchBarCallback {
            override fun onChecked(newStatus: Boolean) {
                setEnabled(newStatus)
            }
        }
        return binding.root
    }

    fun setEnabled(newStatus: Boolean) {
        lifecycleScope.launchWhenResumed {
            val info = getSafetyNetServiceInfo(requireContext())
            val newConfiguration = info.configuration.copy(enabled = newStatus)
            displayServiceInfo(setSafetyNetServiceConfiguration(requireContext(), newConfiguration))
        }
    }

    fun displayServiceInfo(serviceInfo: ServiceInfo) {
        binding.safetynetEnabled = serviceInfo.configuration.enabled
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            binding.checkinEnabled = getCheckinServiceInfo(requireContext()).configuration.enabled
            displayServiceInfo(getSafetyNetServiceInfo(requireContext()))
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
                findNavController().navigate(requireContext(), R.id.openSafetyNetAdvancedSettings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_ADVANCED = Menu.FIRST
    }
}
