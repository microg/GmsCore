/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.location.Location
import android.os.Bundle
import java.security.MessageDigest

const val TAG = "NetworkLocation"
const val LOCATION_EXTRA_PRECISION = "precision"

internal var Location.precision: Double
    get() = extras?.getDouble(LOCATION_EXTRA_PRECISION, 1.0) ?: 1.0
    set(value) {
        extras = (extras ?: Bundle()).apply { putDouble(LOCATION_EXTRA_PRECISION, value) }
    }

fun ByteArray.toHexString(separator: String = "") : String = joinToString(separator) { "%02x".format(it) }
fun ByteArray.digest(md: String): ByteArray = MessageDigest.getInstance(md).digest(this)