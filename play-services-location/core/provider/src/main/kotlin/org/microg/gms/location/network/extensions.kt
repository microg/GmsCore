/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.location.Location
import android.os.Bundle
import androidx.core.location.LocationCompat
import androidx.core.os.bundleOf
import java.security.MessageDigest

const val TAG = "NetworkLocation"
const val LOCATION_EXTRA_PRECISION = "precision"

const val PROVIDER_CACHE = "cache"
const val PROVIDER_CACHE_NEGATIVE = "cache-"
val NEGATIVE_CACHE_ENTRY = Location(PROVIDER_CACHE_NEGATIVE)

internal operator fun <T> Bundle?.plus(pair: Pair<String, T>): Bundle = this + bundleOf(pair)

internal operator fun Bundle?.plus(other: Bundle): Bundle = when {
    this == null -> other
    else -> Bundle(this).apply { putAll(other) }
}

internal var Location.precision: Double
    get() = extras?.getDouble(LOCATION_EXTRA_PRECISION, 1.0) ?: 1.0
    set(value) {
        extras += LOCATION_EXTRA_PRECISION to value
    }

internal var Location.verticalAccuracy: Float?
    get() = if (LocationCompat.hasVerticalAccuracy(this)) LocationCompat.getVerticalAccuracyMeters(this) else null
    set(value) = if (value == null) LocationCompat.removeVerticalAccuracy(this) else LocationCompat.setVerticalAccuracyMeters(this, value)

fun ByteArray.toHexString(separator: String = "") : String = joinToString(separator) { "%02x".format(it) }
fun ByteArray.digest(md: String): ByteArray = MessageDigest.getInstance(md).digest(this)