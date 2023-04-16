/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager

class VersionUtil(private val context: Context) {
    val buildType: String
        get() {
            ProfileManager.ensureInitialized(context)
            // Note: Android TV and Watch use different version codes
            val versionCode = when (Build.VERSION.SDK_INT) {
                31 -> "19"
                30 -> "15"
                29 -> "12"
                28 -> "10"
                23, 24, 25, 26, 27 -> "04"
                21, 22 -> "02"
                else -> "00"
            }
            val architectureCode = when (Build.CPU_ABI) {
                "x86_64" -> "08"
                "x86" -> "07"
                "arm64-v8a" -> "04"
                "arm", "armeabi", "armeabi-v7a" -> "03"
                else -> "00"
            }
            val dpiCode = when (context.resources.displayMetrics.densityDpi) { // TODO: Also something to get from profile
                160 -> "02"
                240 -> "04"
                320 -> "06"
                480 -> "08"
                else -> "00"
            }
            val type = "$versionCode$architectureCode$dpiCode"
            if (isKnown(type)) return type
            val nodpi = "$versionCode${architectureCode}00"
            if (isKnown(nodpi)) return nodpi // Fallback to nodpi for increased compat
            return type // Use unknown build type
        }
    val versionString: String
        get() = "${BuildConfig.VERSION_NAME} ($buildType-{{cl}})"
    val versionCode: Int
        get() = BuildConfig.VERSION_CODE + (getVersionOffset(buildType) ?: 0)

    fun isKnown(type: String): Boolean = getVersionOffset(type) != null

    fun getVersionOffset(type: String): Int? {
        val v1 = type.substring(0, 2)
        val v2 = type.substring(2, 4)
        val v3 = type.substring(4, 6)
        val i1 = BUILD_MAP.indexOfFirst { it.first == v1 }.takeIf { it >= 0 } ?: return null
        val i2 = BUILD_MAP[i1].second.indexOfFirst { it.first == v2 }.takeIf { it >= 0 } ?: return null
        val i3 = BUILD_MAP[i1].second[i2].second.indexOf(v3).takeIf { it > 0 } ?: return null
        val o1 = BUILD_MAP.subList(0, i1).map { it.second.map { it.second.size }.sum() }.sum()
        val o2 = BUILD_MAP[i1].second.subList(0, i2).map { it.second.size }.sum()
        return o1 + o2 + i3
    }

    companion object {
        val BUILD_MAP = listOf(
                "00" to listOf("03" to listOf("00", "02", "04", "06", "08"), "07" to listOf("00")),
                "02" to listOf("03" to listOf("00", "04", "06", "08"), "04" to listOf("00", "06", "08"), "07" to listOf("00"), "08" to listOf("00")),
                "04" to listOf("03" to listOf("00", "04", "06", "08"), "04" to listOf("00", "06", "08"), "07" to listOf("00"), "08" to listOf("00")),
                "10" to listOf("03" to listOf("00", "04", "06", "08"), "04" to listOf("00", "06", "08"), "07" to listOf("00"), "08" to listOf("00")),
                "12" to listOf("03" to listOf("00", "04", "06", "08"), "04" to listOf("00", "06", "08"), "07" to listOf("00"), "08" to listOf("00")),
                "15" to listOf("03" to listOf("00", "04", "06", "08"), "04" to listOf("00", "06", "08"), "07" to listOf("00"), "08" to listOf("00")),
                "19" to listOf("03" to listOf("00", "08"), "04" to listOf("00", "08"), "07" to listOf("00"), "08" to listOf("00")),
        )
    }
}
