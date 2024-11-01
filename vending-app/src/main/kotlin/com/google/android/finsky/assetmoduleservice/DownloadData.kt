/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.assetmoduleservice

import android.os.Bundle
import java.io.Serializable

data class DownloadData(
    var packageName: String = "",
    var errorCode: Int = 0,
    var sessionIds: Map<String, Int> = emptyMap(),
    var bytesDownloaded: Long = 0,
    var status: Int = 0,
    var moduleNames: Set<String> = emptySet(),
    var appVersionCode: Long = 0,
    var totalBytesToDownload: Long = 0,
    var moduleDataList: Map<String, ModuleData> = emptyMap()
) : Serializable {

    fun getModuleData(packName: String): ModuleData {
        return moduleDataList[packName] ?: throw IllegalArgumentException("ModuleData for packName '$packName' not found.")
    }

    fun incrementModuleBytesDownloaded(packName: String, bytes: Long) {
        getModuleData(packName).incrementBytesDownloaded(bytes)
        bytesDownloaded += bytes
    }

    fun updateDownloadStatus(packName: String, statusCode: Int) {
        getModuleData(packName).apply {
            status = statusCode
            sessionId = statusCode
        }
    }

    fun updateModuleDownloadStatus(statusCode: Int) {
        this.status = statusCode
    }
}

data class ModuleData(
    var appVersionCode: Long = 0,
    var moduleVersion: Long = 0,
    var sessionId: Int = 0,
    var errorCode: Int = 0,
    var status: Int = 0,
    var bytesDownloaded: Long = 0,
    var totalBytesToDownload: Long = 0,
    var packBundleList: List<Bundle> = emptyList(),
    var listOfSubcontractNames: ArrayList<String>? = null
) : Serializable {
    fun incrementBytesDownloaded(bytes: Long) {
        bytesDownloaded += bytes
    }
}