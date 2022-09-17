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
import org.microg.gms.checkin.CheckinPrefs
import org.microg.gms.droidguard.core.DroidGuardPreferences
import org.microg.gms.safetynet.SafetyNetDatabase
import org.microg.gms.safetynet.SafetyNetPreferences

class SafetyNetFragment : Fragment(R.layout.safety_net_fragment) {

    private lateinit var binding: SafetyNetFragmentBinding

    init {
        setHasOptionsMenu(true)
    }

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
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            SafetyNetPreferences.setEnabled(appContext, newStatus)
            DroidGuardPreferences.setEnabled(appContext, newStatus)
            displayServiceInfo()
        }
    }

    fun displayServiceInfo() {
        binding.safetynetEnabled = SafetyNetPreferences.isEnabled(requireContext()) && DroidGuardPreferences.isEnabled(requireContext())
    }

    override fun onResume() {
        super.onResume()
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            binding.checkinEnabled = CheckinPrefs.isEnabled(appContext)
            displayServiceInfo()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_ADVANCED, 0, R.string.menu_advanced)
        menu.add(0, MENU_CLEAR_REQUESTS, 0, R.string.menu_clear_recent_requests)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ADVANCED -> {
                findNavController().navigate(requireContext(), R.id.openSafetyNetAdvancedSettings)
                true
            }
            MENU_CLEAR_REQUESTS -> {
                val db = SafetyNetDatabase(requireContext())
                db.clearAllRequests()
                db.close()
                (childFragmentManager.findFragmentById(R.id.sub_preferences) as? SafetyNetPreferencesFragment)?.updateContent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_ADVANCED = Menu.FIRST
        private const val MENU_CLEAR_REQUESTS = Menu.FIRST + 1
    }
}
