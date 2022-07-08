/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.os.Bundle
import org.microg.gms.fido.core.ui.AuthenticatorActivityFragmentData.Companion.KEY_IS_FIRST

class AuthenticatorActivityFragmentData(val arguments: Bundle) {
    val appName: String?
        get() = arguments.getString(KEY_APP_NAME)

    val facetId: String?
        get() = arguments.getString(KEY_FACET_ID)

    val isFirst: Boolean
        get() = arguments.getBoolean(KEY_IS_FIRST) ?: true

    val supportedTransports: Set<Transport>
        get() = arguments.getStringArrayList(KEY_SUPPORTED_TRANSPORTS)?.map { Transport.valueOf(it) }?.toSet().orEmpty()

    companion object {
        const val KEY_APP_NAME = "appName"
        const val KEY_FACET_ID = "facetId"
        const val KEY_IS_FIRST = "isFirst"
        const val KEY_SUPPORTED_TRANSPORTS = "supportedTransports"
    }
}

fun Bundle?.withIsFirst(isFirst: Boolean) = Bundle(this ?: Bundle.EMPTY).apply { putBoolean(KEY_IS_FIRST, isFirst) }
