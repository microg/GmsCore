/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vending

import org.json.JSONObject

enum class AllowType(val value: Int) {
    ALLOWED_NEVER(0),
    ALLOWED_REQUEST(1),
    ALLOWED_SINGLE(2),
    ALLOWED_ALWAYS(3),
}

data class InstallChannelData(val packageName: String, var allowType: Int, var pkgSignSha256: String) {

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

        private fun parse(json: String): InstallChannelData {
            val json = JSONObject(json)
            return InstallChannelData(
                json.getString(CHANNEL_PACKAGE_NAME),
                json.getInt(CHANNEL_ALLOW_TYPE),
                json.getString(CHANNEL_SIGNATURE)
            )
        }

        fun loadChannelDataSet(content: String): MutableSet<InstallChannelData> {
            val localChannelList = mutableSetOf<InstallChannelData>()
            if (content.isNotEmpty()) {
                content.split("|").map {
                    parse(it)
                }.forEach {
                    localChannelList.add(it)
                }
            }
            return localChannelList
        }

        fun updateLocalChannelData(localChannelList: MutableSet<InstallChannelData>, channel: InstallChannelData): String {
            val channelData = localChannelList.find { it.packageName == channel.packageName && it.pkgSignSha256 == channel.pkgSignSha256 }
            if (channelData != null) {
                channelData.allowType = channel.allowType
            } else {
                localChannelList.add(channel)
            }
            return localChannelList.let { it -> it.joinToString(separator = "|") { it.toString() } }
        }
    }
}