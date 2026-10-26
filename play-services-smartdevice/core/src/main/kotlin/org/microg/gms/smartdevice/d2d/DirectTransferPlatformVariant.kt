/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.content.Context
import android.os.Build
import android.os.UserManager

private const val DEFAULT = 0
private const val AMATI = 2
private const val COMMUNAL = 5
private const val COMMUNAL_MODE_FEATURE = "android.software.communal_mode"
private const val COMMUNAL_PROFILE_TYPE = "android.os.usertype.profile.COMMUNAL"
private const val AMATI_EXPERIENCE_FEATURE = "com.google.android.feature.AMATI_EXPERIENCE"
private val MANUFACTURER_BLOCKLIST = setOf("TCL")

internal fun Context.detectDirectTransferPlatformVariant(): Int {
    if (Build.MANUFACTURER in MANUFACTURER_BLOCKLIST) return DEFAULT
    val packageManager = packageManager
    if (packageManager.hasSystemFeature(COMMUNAL_MODE_FEATURE) && isCommunalProfile()) {
        return COMMUNAL
    }
    return if (packageManager.hasSystemFeature(AMATI_EXPERIENCE_FEATURE)) AMATI else DEFAULT
}

private fun Context.isCommunalProfile(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    val userManager = getSystemService(Context.USER_SERVICE) as? UserManager ?: return false
    return runCatching {
        UserManager::class.java
            .getMethod("isUserOfType", String::class.java)
            .invoke(userManager, COMMUNAL_PROFILE_TYPE) as? Boolean == true
    }.getOrDefault(false)
}
