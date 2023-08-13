/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.Log
import androidx.core.location.LocationCompat

const val ACTION_NETWORK_LOCATION_SERVICE = "org.microg.gms.location.network.ACTION_NETWORK_LOCATION_SERVICE"
const val EXTRA_LOCATION = "location"
const val EXTRA_PENDING_INTENT = "pending_intent"
const val EXTRA_ENABLE = "enable"
const val EXTRA_INTERVAL_MILLIS = "interval"
const val EXTRA_FORCE_NOW = "force_now"
const val EXTRA_LOW_POWER = "low_power"
const val EXTRA_WORK_SOURCE = "work_source"
const val EXTRA_BYPASS = "bypass"

val Location.elapsedMillis: Long
    get() = LocationCompat.getElapsedRealtimeMillis(this)

fun Long.formatRealtime(): CharSequence = if (this <= 0) "n/a" else DateUtils.getRelativeTimeSpanString((this - SystemClock.elapsedRealtime()) + System.currentTimeMillis(), System.currentTimeMillis(), 0)
fun Long.formatDuration(): CharSequence {
    if (this == 0L) return "0ms"
    if (this > 315360000000L /* ten years */) return "\u221e"
    val interval = listOf(1000, 60, 60, 24, Long.MAX_VALUE)
    val intervalName = listOf("ms", "s", "m", "h", "d")
    var ret = ""
    var rem = this
    for (i in 0 until interval.size) {
        val mod = rem % interval[i]
        if (mod != 0L) {
            ret = "$mod${intervalName[i]}$ret"
        }
        rem /= interval[i]
        if (mod == 0L && rem == 1L) {
            ret = "${interval[i]}${intervalName[i]}$ret"
            break
        } else if (rem == 0L) {
            break
        }
    }
    return ret
}

private var hasMozillaLocationServiceSupportFlag: Boolean? = null
fun Context.hasMozillaLocationServiceSupport(): Boolean {
    if (!hasNetworkLocationServiceBuiltIn()) return false
    var flag = hasMozillaLocationServiceSupportFlag
    if (flag == null) {
        return try {
            val clazz = Class.forName("org.microg.gms.location.network.mozilla.MozillaLocationServiceClient")
            val apiKey = clazz.getDeclaredField("API_KEY").get(null) as? String?
            flag = apiKey != null
            hasMozillaLocationServiceSupportFlag = flag
            flag
        } catch (e: Exception) {
            Log.w("Location", e)
            hasMozillaLocationServiceSupportFlag = false
            false
        }
    } else {
        return flag
    }
}

private var hasNetworkLocationServiceBuiltInFlag: Boolean? = null
fun Context.hasNetworkLocationServiceBuiltIn(): Boolean {
    var flag = hasNetworkLocationServiceBuiltInFlag
    if (flag == null) {
        try {
            val serviceIntent = Intent().apply {
                action = ACTION_NETWORK_LOCATION_SERVICE
                setPackage(packageName)
            }
            val services = packageManager?.queryIntentServices(serviceIntent, PackageManager.MATCH_DEFAULT_ONLY)
            flag = services?.isNotEmpty() ?: false
            hasNetworkLocationServiceBuiltInFlag = flag
            return flag
        } catch (e: Exception) {
            hasNetworkLocationServiceBuiltInFlag = false
            return false
        }
    } else {
        return flag
    }
}