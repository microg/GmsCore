/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.databinding.FidoNfcTransportFragmentBinding
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandlerCallback

class NfcTransportFragment : AuthenticatorActivityFragment(), TransportHandlerCallback {
    private lateinit var binding: FidoNfcTransportFragmentBinding
    private var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FidoNfcTransportFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        binding.onBackClick = View.OnClickListener {
            if (!findNavController().navigateUp()) {
                findNavController().navigate(
                    R.id.transportSelectionFragment,
                    arguments,
                    navOptions { popUpTo(R.id.usbFragment) { inclusive = true } })
            }
        }
        if (SDK_INT >= 23) {
            (binding.fidoNfcWaitConnectAnimation.drawable as? AnimatedVectorDrawable)?.registerAnimationCallback(object :
                Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    lifecycleScope.launchWhenStarted {
                        delay(250)
                        (drawable as? AnimatedVectorDrawable)?.reset()
                        delay(500)
                        (drawable as? AnimatedVectorDrawable)?.start()
                    }
                }
            })
            (binding.fidoNfcWaitConnectAnimation.drawable as? AnimatedVectorDrawable)?.start()
        }
        return binding.root
    }

    override fun onStatusChanged(transport: Transport, status: String, extras: Bundle?) {
        if (transport != Transport.NFC) return
        binding.status = status
    }

    override fun onStart() {
        super.onStart()
        job = startTransportHandling(Transport.NFC)
    }

    override fun onStop() {
        job?.cancel()
        super.onStop()
    }
}
