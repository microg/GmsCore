/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

fun PackageManager.getApplicationInfoIfExists(packageName: String?, flags: Int = 0): ApplicationInfo? = packageName?.let {
    try {
        getApplicationInfo(it, flags)
    } catch (e: Exception) {
        Log.w(TAG, "Package does not exist", e)
        null
    }
}
