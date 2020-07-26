/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.google.android.gms.R
import com.google.android.gms.databinding.PushNotificationFragmentBinding
import org.microg.gms.checkin.CheckinPrefs
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.gcm.McsService
import org.microg.gms.gcm.TriggerReceiver

class PushNotificationFragment : Fragment(R.layout.push_notification_fragment) {
    lateinit var binding: PushNotificationFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PushNotificationFragmentBinding.inflate(inflater, container, false)
        binding.switchBarCallback = object : PreferenceSwitchBarCallback {
            override fun onChecked(newStatus: Boolean) {
                GcmPrefs.setEnabled(context, newStatus)
                binding.gcmEnabled = newStatus
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            binding.gcmEnabled = GcmPrefs.get(context).isEnabled
            binding.checkinEnabled = CheckinPrefs.get(context).isEnabled
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
                findNavController().navigate(R.id.openGcmAdvancedSettings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_ADVANCED = Menu.FIRST
    }
}

