/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.annotation.TargetApi
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.transport.Transport

@TargetApi(24)
abstract class AuthenticatorActivityFragment : Fragment() {
    private val pinViewModel: AuthenticatorPinViewModel by activityViewModels()
    val data: AuthenticatorActivityFragmentData
        get() = AuthenticatorActivityFragmentData(arguments ?: Bundle.EMPTY)
    val authenticatorActivity: AuthenticatorActivity?
        get() = activity as? AuthenticatorActivity
    val options: RequestOptions?
        get() = authenticatorActivity?.options

    fun startTransportHandling(transport: Transport) = authenticatorActivity?.startTransportHandling(transport, pinRequested = pinViewModel.pinRequest, authenticatorPin = pinViewModel.pin)
    fun shouldStartTransportInstantly(transport: Transport) = authenticatorActivity?.shouldStartTransportInstantly(transport) == true

    abstract override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
}
