/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport

import android.os.Bundle
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import org.microg.gms.fido.core.RequestHandlingException

abstract class TransportHandler(val transport: Transport, val callback: TransportHandlerCallback?) {
    open val isSupported: Boolean
        get() = false

    open suspend fun start(options: RequestOptions, callerPackage: String): AuthenticatorResponse =
        throw RequestHandlingException(ErrorCode.NOT_SUPPORTED_ERR)

    open fun shouldBeUsedInstantly(options: RequestOptions): Boolean = false
    fun invokeStatusChanged(status: String, extras: Bundle? = null) =
        callback?.onStatusChanged(transport, status, extras)
}

interface TransportHandlerCallback {
    fun onStatusChanged(transport: Transport, status: String, extras: Bundle? = null)

    companion object {
        @JvmStatic
        val STATUS_WAITING_FOR_DEVICE = "waiting-for-device"
        @JvmStatic
        val STATUS_WAITING_FOR_USER = "waiting-for-user"
        @JvmStatic
        val STATUS_UNKNOWN = "unknown"
    }
}
