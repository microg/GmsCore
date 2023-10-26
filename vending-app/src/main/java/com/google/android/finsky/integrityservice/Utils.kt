/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.integrityservice

import android.os.Bundle
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import com.google.android.play.core.integrity.protocol.IIntegrityServiceCallback

fun IIntegrityServiceCallback.onRequestIntegrityToken(@IntegrityErrorCode error: Int = IntegrityErrorCode.NO_ERROR) {
    onRequestIntegrityToken(Bundle().apply {
        putInt("error", error)
    })
}

fun IIntegrityServiceCallback.onRequestIntegrityToken(token: String) {
    onRequestIntegrityToken(Bundle().apply {
        putString("token", token)
    })
}
