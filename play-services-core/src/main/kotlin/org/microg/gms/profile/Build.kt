/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.profile

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import kotlin.random.Random

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
    }
}
