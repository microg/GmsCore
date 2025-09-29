/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vending

import org.json.JSONObject

enum class AllowType(val value: Int){
    ALLOWED_NEVER(0),
    ALLOWED_REQUEST(1),
    ALLOWED_SINGLE(2),
    ALLOWED_ALWAYS(3),
}

data class InstallChannelData(val packageName: String, var allowed: Int) {

    override fun toString(): String {
        return JSONObject().put(CHANNEL_PACKAGE_NAME, packageName).put(CHANNEL_ALLOWED, allowed).toString()
    }

    companion object {
        private const val CHANNEL_PACKAGE_NAME = "packageName"
        private const val CHANNEL_ALLOWED = "allowed"
        private val localChannelList = mutableSetOf<InstallChannelData>()

        private fun parse(json: String): InstallChannelData {
            val json = JSONObject(json)
            return InstallChannelData(json.getString(CHANNEL_PACKAGE_NAME), json.getInt(CHANNEL_ALLOWED))
        }

        fun loadChannelDataSet(content: String): Set<InstallChannelData> {
            if (content.isEmpty()) {
                return emptySet()
            }
            localChannelList.clear()
            content.split("|").map {
                parse(it)
            }.forEach {
                localChannelList.add(it)
            }
            return localChannelList
        }

        fun updateLocalChannelData(channelPackage: String, allowType: Int): String {
            val channelData = localChannelList.find { it.packageName == channelPackage }
            if (channelData != null) {
                channelData.allowed = allowType
            } else {
                localChannelList.add(InstallChannelData(channelPackage, allowType))
            }
            return localChannelList.let { it -> it.joinToString(separator = "|") { it.toString() } }
        }
    }
}