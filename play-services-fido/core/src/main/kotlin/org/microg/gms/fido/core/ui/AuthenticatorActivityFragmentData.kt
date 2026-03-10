/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.os.Bundle
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.ui.AuthenticatorActivityFragmentData.Companion.KEY_IS_FIRST

class AuthenticatorActivityFragmentData(val arguments: Bundle = Bundle()) {
    var appName: String?
        get() = arguments.getString(KEY_APP_NAME)
        set(value) = arguments.putString(KEY_APP_NAME, value)

    var isFirst: Boolean
        get() = arguments.getBoolean(KEY_IS_FIRST, true)
        set(value) = arguments.putBoolean(KEY_IS_FIRST, value)

    var supportedTransports: Set<Transport>
        get() = arguments.getStringArrayList(KEY_SUPPORTED_TRANSPORTS)?.map { Transport.valueOf(it) }?.toSet().orEmpty()
        set(value) = arguments.putStringArrayList(KEY_SUPPORTED_TRANSPORTS, ArrayList(value.map { it.name }))

    val implementedTransports: Set<Transport>
        get() = AuthenticatorActivity.IMPLEMENTED_TRANSPORTS

    var privilegedCallerName: String?
        get() = arguments.getString(KEY_PRIVILEGED_CALLER_NAME)
        set(value) = arguments.putString(KEY_PRIVILEGED_CALLER_NAME, value)

    var requiresPrivilege: Boolean
        get() = arguments.getBoolean(KEY_REQUIRES_PRIVILEGE)
        set(value) = arguments.putBoolean(KEY_REQUIRES_PRIVILEGE, value)

    companion object {
        const val KEY_APP_NAME = "appName"
        const val KEY_IS_FIRST = "isFirst"
        const val KEY_SUPPORTED_TRANSPORTS = "supportedTransports"
        const val KEY_REQUIRES_PRIVILEGE = "requiresPrivilege"
        const val KEY_PRIVILEGED_CALLER_NAME = "privilegedCallerName"
    }
}

fun Bundle?.withIsFirst(isFirst: Boolean) = Bundle(this ?: Bundle.EMPTY).apply { putBoolean(KEY_IS_FIRST, isFirst) }
