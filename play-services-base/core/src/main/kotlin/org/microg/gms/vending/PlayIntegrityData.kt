/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vending

import org.json.JSONException
import org.json.JSONObject

class PlayIntegrityData(var allowed: Boolean,
                        val packageName: String,
                        val pkgSignSha256: String,
                        var lastTime: Long,
                        var lastResult: String? = null,
                        var lastStatus: Boolean = false) {

    override fun toString(): String {
        return JSONObject()
            .put(ALLOWED, allowed)
            .put(PACKAGE_NAME, packageName)
            .put(SIGNATURE, pkgSignSha256)
            .put(LAST_VISIT_TIME, lastTime)
            .put(LAST_VISIT_RESULT, lastResult)
            .put(LAST_VISIT_STATUS, lastStatus)
            .toString()
    }

    companion object {
        private const val PACKAGE_NAME = "packageName"
        private const val ALLOWED = "allowed"
        private const val SIGNATURE = "signature"
        private const val LAST_VISIT_TIME = "lastVisitTime"
        private const val LAST_VISIT_RESULT = "lastVisitResult"
        private const val LAST_VISIT_STATUS = "lastVisitStatus"

        private fun parse(jsonString: String): PlayIntegrityData? {
            try {
                val json = JSONObject(jsonString)
                return PlayIntegrityData(
                    json.getBoolean(ALLOWED),
                    json.getString(PACKAGE_NAME),
                    json.getString(SIGNATURE),
                    json.getLong(LAST_VISIT_TIME),
                    json.getString(LAST_VISIT_RESULT),
                    json.getBoolean(LAST_VISIT_STATUS)
                )
            } catch (e: JSONException) {
                return null
            }
        }

        fun loadDataSet(content: String): Set<PlayIntegrityData> {
            return content.split("|").mapNotNull { parse(it) }.toSet()
        }

        fun updateDataSetString(channelList: Set<PlayIntegrityData>, channel: PlayIntegrityData): String {
            val channelData = channelList.find { it.packageName == channel.packageName && it.pkgSignSha256 == channel.pkgSignSha256 }
            val newChannelList = if (channelData != null) {
                channelData.allowed = channel.allowed
                channelData.lastTime = channel.lastTime
                channelData.lastResult = channel.lastResult
                channelData.lastStatus = channel.lastStatus
                channelList
            } else {
                channelList + channel
            }
            return newChannelList.let { it -> it.joinToString(separator = "|") { it.toString() } }
        }
    }
}