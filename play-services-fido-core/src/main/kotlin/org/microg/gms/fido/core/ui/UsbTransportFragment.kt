/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.databinding.FidoUsbTransportFragmentBinding
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandlerCallback

class UsbTransportFragment : AuthenticatorActivityFragment(), TransportHandlerCallback {
    private lateinit var binding: FidoUsbTransportFragmentBinding
    private var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FidoUsbTransportFragmentBinding.inflate(inflater, container, false)
        binding.data = data
        binding.onBackClick = View.OnClickListener {
            if (!findNavController().navigateUp()) {
                findNavController().navigate(
                    R.id.transportSelectionFragment,
                    arguments,
                    navOptions { popUpTo(R.id.usbFragment) { inclusive = true } })
            }
        }
        if (Build.VERSION.SDK_INT >= 23) {
            for (imageView in listOfNotNull(binding.fidoUsbWaitConnectAnimation, binding.fidoUsbWaitConfirmAnimation)) {
                (imageView.drawable as? AnimatedVectorDrawable)?.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        lifecycleScope.launchWhenStarted {
                            delay(250)
                            (drawable as? AnimatedVectorDrawable)?.reset()
                            delay(500)
                            (drawable as? AnimatedVectorDrawable)?.start()
                        }
                    }
                })
                (imageView.drawable as? AnimatedVectorDrawable)?.start()
            }
        }
        return binding.root
    }

    override fun onStatusChanged(transport: Transport, status: String, extras: Bundle?) {
        if (transport != Transport.USB) return
        binding.status = status
        if (Build.VERSION.SDK_INT >= 21) {
            binding.deviceName =
                extras?.getParcelable<UsbDevice>(UsbManager.EXTRA_DEVICE)?.productName ?: "your security key"
        } else {
            binding.deviceName = "your security key"
        }
    }

    override fun onStart() {
        super.onStart()
        job = startTransportHandling(Transport.USB)
    }

    override fun onStop() {
        job?.cancel()
        super.onStop()
    }
}
