/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.assetmoduleservice

import android.os.Bundle
import androidx.collection.ArrayMap

data class ModuleData(
    var packageName: String? = null,
    var errorCode: Int = 0,
    var sessionIds: ArrayMap<String, Int>? = null,
    var bytesDownloaded: Long = 0,
    var status: Int = 0,
    var packNames: ArrayList<String>? = null,
    var appVersionCode: Int = 0,
    var totalBytesToDownload: Long = 0
) {
    private var mPackData = emptyMap<String, PackData>()

    fun setPackData(packData: Map<String, PackData>) {
        this.mPackData = packData
    }

    fun getPackData(packName: String): PackData? {
        return mPackData[packName]
    }

    fun incrementPackBytesDownloaded(packName: String, bytes: Long) {
        mPackData[packName]?.incrementBytesDownloaded(bytes)
    }

    fun incrementBytesDownloaded(packName: String) {
        bytesDownloaded += getPackData(packName)?.bytesDownloaded ?: 0
    }

    fun updateDownloadStatus(packName: String, statusCode: Int) {
        getPackData(packName)?.status = statusCode
        getPackData(packName)?.sessionId = statusCode
    }

    fun updateModuleDownloadStatus(statusCode: Int) {
        this.status = statusCode
    }
}

data class PackData(
    var packVersion: Int = 0,
    var packBaseVersion: Int = 0,
    var sessionId: Int = 0,
    var errorCode: Int = 0,
    var status: Int = 0,
    var bytesDownloaded: Long = 0,
    var totalBytesToDownload: Long = 0,
    var packVersionTag: String? = null,
    var bundleList: ArrayList<Bundle>? = null,
    var totalSumOfSubcontractedModules: Int = 0,
    var subcontractingBaseUnit: Int = 0,
    var listOfSubcontractNames: ArrayList<String>? = null
) {
    fun incrementBytesDownloaded(bytes: Long) {
        bytesDownloaded += bytes
    }
}
