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
import androidx.core.location.LocationCompat

const val ACTION_NETWORK_LOCATION_SERVICE = "org.microg.gms.location.network.ACTION_NETWORK_LOCATION_SERVICE"
const val EXTRA_LOCATION = "location"
const val EXTRA_ELAPSED_REALTIME = "elapsed_realtime"
const val EXTRA_PENDING_INTENT = "pending_intent"
const val EXTRA_ENABLE = "enable"
const val EXTRA_INTERVAL_MILLIS = "interval"
const val EXTRA_FORCE_NOW = "force_now"
const val EXTRA_LOW_POWER = "low_power"
const val EXTRA_WORK_SOURCE = "work_source"
const val EXTRA_BYPASS = "bypass"

const val ACTION_CONFIGURATION_REQUIRED = "org.microg.gms.location.network.ACTION_CONFIGURATION_REQUIRED"
const val EXTRA_CONFIGURATION = "config"
const val CONFIGURATION_FIELD_ONLINE_SOURCE = "online_source"

const val ACTION_NETWORK_IMPORT_EXPORT = "org.microg.gms.location.network.ACTION_NETWORK_IMPORT_EXPORT"
const val EXTRA_DIRECTION = "direction"
const val DIRECTION_IMPORT = "import"
const val DIRECTION_EXPORT = "export"
const val EXTRA_NAME = "name"
const val NAME_WIFI = "wifi"
const val NAME_CELL = "cell"
const val EXTRA_URI = "uri"
const val EXTRA_MESSENGER = "messenger"
const val EXTRA_REPLY_WHAT = "what"

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