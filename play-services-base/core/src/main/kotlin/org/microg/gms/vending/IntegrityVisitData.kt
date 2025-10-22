/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vending

import org.json.JSONException
import org.json.JSONObject

class IntegrityVisitData(var allowed: Boolean, val packageName: String, val pkgSignSha256: String) {
    var lastVisitTime: Long? = null
    var lastVisitResult: String? = null

    override fun toString(): String {
        return JSONObject()
            .put(ALLOWED, allowed)
            .put(PACKAGE_NAME, packageName)
            .put(SIGNATURE, pkgSignSha256)
            .put(LAST_VISIT_TIME, lastVisitTime)
            .put(LAST_VISIT_RESULT, lastVisitResult)
            .toString()
    }

    companion object {
        private const val PACKAGE_NAME = "packageName"
        private const val ALLOWED = "allowed"
        private const val SIGNATURE = "signature"
        private const val LAST_VISIT_TIME = "lastVisitTime"
        private const val LAST_VISIT_RESULT = "lastVisitResult"

        private fun parse(jsonString: String): IntegrityVisitData? {
            try {
                val json = JSONObject(jsonString)
                return IntegrityVisitData(
                    json.getBoolean(ALLOWED),
                    json.getString(PACKAGE_NAME),
                    json.getString(SIGNATURE)
                ).apply {
                    lastVisitTime = json.getLong(LAST_VISIT_TIME)
                    lastVisitResult = json.getString(LAST_VISIT_RESULT)
                }
            } catch (e: JSONException) {
                return null
            }
        }

        fun loadDataSet(content: String): Set<IntegrityVisitData> {
            return content.split("|").mapNotNull { parse(it) }.toSet()
        }

        fun updateDataSetString(channelList: Set<IntegrityVisitData>, channel: IntegrityVisitData): String {
            val channelData = channelList.find { it.packageName == channel.packageName && it.pkgSignSha256 == channel.pkgSignSha256 }
            val newChannelList = if (channelData != null) {
                channelData.allowed = channel.allowed
                channelData.lastVisitTime = channel.lastVisitTime
                channelData.lastVisitResult = channel.lastVisitResult
                channelList
            } else {
                channelList + channel
            }
            return newChannelList.let { it -> it.joinToString(separator = "|") { it.toString() } }
        }
    }
}