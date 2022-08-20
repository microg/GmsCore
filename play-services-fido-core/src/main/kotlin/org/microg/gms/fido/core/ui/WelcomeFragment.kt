/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.databinding.FidoWelcomeFragmentBinding

class WelcomeFragment : AuthenticatorActivityFragment() {
    private lateinit var binding: FidoWelcomeFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FidoWelcomeFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        binding.onGetStartedClick = View.OnClickListener {
            for (transport in data.supportedTransports) {
                if (shouldStartTransportInstantly(transport)) {
                    startTransportHandling(transport)
                    return@OnClickListener
                }
            }
            val next = data.supportedTransports.singleOrNull()?.let {
                when (it) {
                    Transport.BLUETOOTH -> R.id.openBluetoothFragmentDirect
                    Transport.NFC -> R.id.openNfcFragmentDirect
                    Transport.USB -> R.id.openUsbFragmentDirect
                    Transport.SCREEN_LOCK -> {
                        startTransportHandling(Transport.SCREEN_LOCK)
                        return@OnClickListener
                    }
                }
            } ?: R.id.openTransportSelectionFragment
            findNavController().navigate(next, arguments.withIsFirst(false))
        }
        return binding.root
    }
}
