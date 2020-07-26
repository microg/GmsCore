/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.R
import com.google.android.gms.databinding.SafetyNetFragmentBinding
import org.microg.gms.checkin.CheckinPrefs
import org.microg.gms.snet.SafetyNetPrefs

class SafetyNetFragment : Fragment(R.layout.safety_net_fragment) {

    private lateinit var binding: SafetyNetFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SafetyNetFragmentBinding.inflate(inflater, container, false)
        binding.switchBarCallback = object : PreferenceSwitchBarCallback {
            override fun onChecked(newStatus: Boolean) {
                SafetyNetPrefs.get(requireContext()).isEnabled = newStatus
                binding.safetynetEnabled = newStatus
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.checkinEnabled = CheckinPrefs.get(requireContext()).isEnabled
        binding.safetynetEnabled = SafetyNetPrefs.get(requireContext()).isEnabled
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
                findNavController().navigate(R.id.openSafetyNetAdvancedSettings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_ADVANCED = Menu.FIRST
    }
}
