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

data class InstallChannelData(val packageName: String, var allowed: Int, var pkgSignSha256:String) {

    override fun toString(): String {
        return JSONObject().put(CHANNEL_PACKAGE_NAME, packageName).put(CHANNEL_ALLOWED, allowed).toString()
    }

    companion object {
        private const val CHANNEL_PACKAGE_NAME = "packageName"
        private const val CHANNEL_ALLOWED = "allowed"
        private const val CHANNEL_PACKAGE_SIGN = "allowed"

        private fun parse(json: String): InstallChannelData {
            val json = JSONObject(json)
            return InstallChannelData(json.getString(CHANNEL_PACKAGE_NAME), json.getInt(CHANNEL_ALLOWED), json.getString(CHANNEL_PACKAGE_SIGN))
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

        fun updateLocalChannelData(originDataStr:String, channelPackage: String, allowType: Int, pkgSignSha256:String): String {
            val localChannelList = loadChannelDataSet(originDataStr)
            val channelData = localChannelList.find { it.packageName == channelPackage && it.pkgSignSha256 == pkgSignSha256 }
            if (channelData != null) {
                channelData.allowed = allowType
            } else {
                localChannelList.add(InstallChannelData(channelPackage, allowType, pkgSignSha256))
            }
            return localChannelList.let { it -> it.joinToString(separator = "|") { it.toString() } }
        }
    }
}