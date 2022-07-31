/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.microg.gms.fido.core.databinding.FidoUsbTransportFragmentBinding
import org.microg.gms.fido.core.transport.Transport

class UsbTransportFragment : AuthenticatorActivityFragment() {
    private lateinit var binding: FidoUsbTransportFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FidoUsbTransportFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        startTransportHandling(Transport.USB)
    }

    override fun onPause() {
        cancelTransportHandling(Transport.USB)
        super.onPause()
    }
}
