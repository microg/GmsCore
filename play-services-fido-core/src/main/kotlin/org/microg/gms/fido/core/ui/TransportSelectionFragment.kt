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
import org.microg.gms.fido.core.databinding.FidoTransportSelectionFragmentBinding
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.Transport.SCREEN_LOCK

class TransportSelectionFragment : AuthenticatorActivityFragment() {
    private lateinit var binding: FidoTransportSelectionFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FidoTransportSelectionFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        binding.setOnBluetoothClick {
            findNavController().navigate(R.id.openBluetoothFragment, arguments.withIsFirst(false))
        }
        binding.setOnNfcClick {
            findNavController().navigate(R.id.openNfcFragment, arguments.withIsFirst(false))
        }
        binding.setOnUsbClick {
            findNavController().navigate(R.id.openUsbFragment, arguments.withIsFirst(false))
        }
        binding.setOnScreenLockClick {
            startTransportHandling(SCREEN_LOCK)
        }
        return binding.root
    }
}
