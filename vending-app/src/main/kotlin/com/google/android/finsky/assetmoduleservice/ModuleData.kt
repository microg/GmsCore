/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.assetmoduleservice

import android.content.Context
import android.os.Bundle
import androidx.collection.ArrayMap
import com.google.android.finsky.sendBroadcastForExistingFile

data class ModuleData(
    var packageName: String? = null,
    var errorCode: Int = 0,
    var sessionIds: ArrayMap<String, Int>? = null,
    var bytesDownloaded: Long = 0,
    var status: Int = 0,
    var packNames: ArrayList<String>? = null,
    var appVersionCode: Long = 0,
    var totalBytesToDownload: Long = 0,
) {
    private var mPackData = emptyMap<String, PackData>()

    fun setPackData(packData: Map<String, PackData>) {
        this.mPackData = packData
    }

    fun getPackData(packName: String): PackData? {
        return mPackData[packName]
    }

    fun incrementPackBytesDownloaded(context: Context, packName: String, bytes: Long) {
        mPackData[packName]?.incrementBytesDownloaded(bytes)
        bytesDownloaded += bytes
    }

    fun updateDownloadStatus(packName: String, statusCode: Int) {
        getPackData(packName)?.status = statusCode
        getPackData(packName)?.sessionId = statusCode
    }

    fun updateModuleDownloadStatus(statusCode: Int) {
        this.status = statusCode
    }

    override fun toString(): String {
        return "ModuleData(packageName=$packageName, errorCode=$errorCode, sessionIds=$sessionIds, bytesDownloaded=$bytesDownloaded, status=$status, packNames=$packNames, appVersionCode=$appVersionCode, totalBytesToDownload=$totalBytesToDownload, mPackData=$mPackData)"
    }
}

data class PackData(
    var packVersion: Long = 0,
    var packBaseVersion: Long = 0,
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

    override fun toString(): String {
        return "PackData(packVersion=$packVersion, packBaseVersion=$packBaseVersion, sessionId=$sessionId, errorCode=$errorCode, status=$status, bytesDownloaded=$bytesDownloaded, totalBytesToDownload=$totalBytesToDownload, packVersionTag=$packVersionTag, bundleList=$bundleList, totalSumOfSubcontractedModules=$totalSumOfSubcontractedModules, subcontractingBaseUnit=$subcontractingBaseUnit, listOfSubcontractNames=$listOfSubcontractNames)"
    }
}
