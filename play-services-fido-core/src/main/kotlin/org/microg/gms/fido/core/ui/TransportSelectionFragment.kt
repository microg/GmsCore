/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.microg.gms.fido.core.databinding.FidoTransportSelectionFragmentBinding

class TransportSelectionFragment : AuthenticatorActivityFragment() {
    private lateinit var binding: FidoTransportSelectionFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FidoTransportSelectionFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        binding.setOnScreenLockClick {
            startScreenLockHandling()
        }
        return binding.root
    }
}
