/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vending

import org.json.JSONException
import org.json.JSONObject

enum class AllowType(val value: Int) {
    REJECT_ALWAYS(0),
    REJECT_ONCE(1),
    ALLOW_ONCE(2),
    ALLOW_ALWAYS(3),
}

data class InstallerData(val packageName: String, var allowType: Int, val pkgSignSha256: String) {

    override fun toString(): String {
        return JSONObject()
            .put(CHANNEL_PACKAGE_NAME, packageName)
            .put(CHANNEL_ALLOW_TYPE, allowType)
            .put(CHANNEL_SIGNATURE, pkgSignSha256)
            .toString()
    }

    companion object {
        private const val CHANNEL_PACKAGE_NAME = "packageName"
        private const val CHANNEL_ALLOW_TYPE = "allowType"
        private const val CHANNEL_SIGNATURE = "signature"

        private fun parse(jsonString: String): InstallerData? {
            try {
                val json = JSONObject(jsonString)
                return InstallerData(
                    json.getString(CHANNEL_PACKAGE_NAME),
                    json.getInt(CHANNEL_ALLOW_TYPE),
                    json.getString(CHANNEL_SIGNATURE)
                )
            } catch (e: JSONException) {
                return null
            }
        }

        fun loadDataSet(content: String): Set<InstallerData> {
            return content.split("|").mapNotNull { parse(it) }.toSet()
        }

        fun updateDataSetString(channelList: Set<InstallerData>, channel: InstallerData): String {
            val channelData = channelList.find { it.packageName == channel.packageName && it.pkgSignSha256 == channel.pkgSignSha256 }
            val newChannelList = if (channelData != null) {
                channelData.allowType = channel.allowType
                channelList
            } else {
                channelList + channel
            }
            return newChannelList.let { it -> it.joinToString(separator = "|") { it.toString() } }
        }
    }
}