/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.profile

import android.annotation.TargetApi

object Build {
    @JvmField
    var BOARD: String? = null

    @JvmField
    var BOOTLOADER: String? = null

    @JvmField
    var BRAND: String? = null

    @JvmField
    var CPU_ABI: String? = null

    @JvmField
    var CPU_ABI2: String? = null

    @JvmField
    @TargetApi(21)
    var SUPPORTED_ABIS: Array<String> = emptyArray()

    @JvmField
    var DEVICE: String? = null

    @JvmField
    var DISPLAY: String? = null

    @JvmField
    var FINGERPRINT: String? = null

    @JvmField
    var HARDWARE: String? = null

    @JvmField
    var HOST: String? = null

    @JvmField
    var ID: String? = null

    @JvmField
    var MANUFACTURER: String? = null

    @JvmField
    var MODEL: String? = null

    @JvmField
    var PRODUCT: String? = null

    @JvmField
    var RADIO: String? = null

    @JvmField
    var SERIAL: String? = null

    @JvmField
    var TAGS: String? = null

    @JvmField
    var TIME: Long = 0L

    @JvmField
    var TYPE: String? = null

    @JvmField
    var USER: String? = null

    object VERSION {
        @JvmField
        var CODENAME: String? = null

        @JvmField
        var INCREMENTAL: String? = null

        @JvmField
        var RELEASE: String? = null

        @JvmField
        var SDK: String? = null

        @JvmField
        var SDK_INT: Int = 0

        @JvmField
        var SECURITY_PATCH: String? = null

        @JvmField
        var DEVICE_INITIAL_SDK_INT: Int = 0
    }

    object VERSION_CODES {
        const val LOLLIPOP = 21             // Android 5.0
        const val LOLLIPOP_MR1 = 22         // Android 5.1
        const val M = 23                    // Android 6.0 (Marshmallow)
        const val N = 24                    // Android 7.0 (Nougat)
        const val N_MR1 = 25                // Android 7.1
        const val O = 26                    // Android 8.0 (Oreo)
        const val O_MR1 = 27                // Android 8.1
        const val P = 28                    // Android 9 (Pie)
        const val Q = 29                    // Android 10
        const val R = 30                    // Android 11
        const val S = 31                    // Android 12
        const val S_V2 = 32                 // Android 12L
        const val TIRAMISU = 33             // Android 13
        const val UPSIDE_DOWN_CAKE = 34     // Android 14
    }

    fun generateWebViewUserAgentString(original: String): String {
        if (!original.startsWith("Mozilla/5.0 (")) return original
        val closeParen: Int = original.indexOf(')')

        return "Mozilla/5.0 (Linux; Android ${VERSION.RELEASE}; $MODEL Build/$ID; wv)${original.substring(closeParen + 1)}"
    }
}
