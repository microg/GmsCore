/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending

import android.os.Build

// TODO move to gradle?
const val FINSKY_VERSION_NAME = "37.9.18-29 [0] [PR] 571399392"
const val FINSKY_VERSION_CODE = 83791820

private val supportedAbis: Array<String> =
    if (Build.VERSION.SDK_INT >= 21) {
        Build.SUPPORTED_ABIS
    } else {
        @Suppress("DEPRECATION")
        if (Build.CPU_ABI2 == null || Build.CPU_ABI2 == "unknown") {
            arrayOf(Build.CPU_ABI)
        } else {
            arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
        }
    }

// TODO use device profile after https://github.com/microg/GmsCore/pull/2071 is merged
private val userAgentProperties = mapOf(
    "api" to "3",
    "versionCode" to FINSKY_VERSION_CODE.toString(),
    "sdk" to Build.VERSION.SDK_INT.toString(),
    "device" to Build.DEVICE.toString(),
    "hardware" to Build.HARDWARE.toString(),
    "product" to Build.PRODUCT.toString(),
    "platformVersionRelease" to Build.VERSION.RELEASE.toString(),
    "model" to Build.MODEL.toString(),
    "buildId" to Build.ID.toString(),
    "isWideScreen" to "0",
    "supportedAbis" to supportedAbis.joinToString(";"),
)

val FINSKY_USER_AGENT = "Android-Finsky/${FINSKY_VERSION_NAME} (${userAgentProperties.entries.joinToString(",")})"
