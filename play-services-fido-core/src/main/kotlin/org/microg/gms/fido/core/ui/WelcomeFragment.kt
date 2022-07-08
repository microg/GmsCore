/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.databinding.FidoWelcomeFragmentBinding

class WelcomeFragment : AuthenticatorActivityFragment() {
    private lateinit var binding: FidoWelcomeFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FidoWelcomeFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        binding.setOnGetStartedClick {
            val next = data.supportedTransports.singleOrNull()?.let {
                when (data.supportedTransports.first()) {
                    Transport.BLUETOOTH -> R.id.openBluetoothFragmentDirect
                    Transport.NFC -> R.id.openNfcFragmentDirect
                    Transport.USB -> R.id.openUsbFragmentDirect
                    Transport.SCREEN_LOCK -> R.id.openScreenLockFragmentDirect
                }
            } ?: R.id.openTransportSelectionFragment
            findNavController().navigate(next, arguments.withIsFirst(false))
        }
        return binding.root
    }
}
