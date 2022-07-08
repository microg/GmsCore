/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.annotation.TargetApi
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import org.microg.gms.fido.core.*

@TargetApi(24)
abstract class AuthenticatorActivityFragment : Fragment() {
    val data: AuthenticatorActivityFragmentData
        get() = AuthenticatorActivityFragmentData(arguments ?: Bundle.EMPTY)
    val authenticatorActivity: AuthenticatorActivity?
        get() = activity as? AuthenticatorActivity

    fun startScreenLockHandling() = authenticatorActivity?.startScreenLockHandling()

    abstract override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
}
