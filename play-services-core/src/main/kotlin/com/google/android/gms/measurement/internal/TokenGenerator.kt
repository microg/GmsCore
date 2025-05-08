/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.measurement.internal

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Locale

object TokenGenerator {
    private val secureRandom: SecureRandom by lazy { SecureRandom() }

    fun generateHexToken(byteLength: Int = 16): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return String.format(Locale.US, "%032x", BigInteger(1, bytes))
    }
}