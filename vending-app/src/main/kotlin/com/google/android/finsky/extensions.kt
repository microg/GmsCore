/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.collection.ArraySet
import androidx.collection.arrayMapOf
import androidx.collection.arraySetOf
import com.android.vending.licensing.AUTH_TOKEN_SCOPE
import com.android.vending.licensing.getAuthToken
import com.android.vending.licensing.getLicenseRequestHeaders
import com.google.android.finsky.assetmoduleservice.DownloadData
import com.google.android.finsky.assetmoduleservice.ModuleData
import com.google.android.finsky.model.DeviceSyncInfo
import org.microg.gms.auth.AuthConstants
import org.microg.vending.billing.GServices
import org.microg.vending.billing.core.HttpClient
import java.io.File
import java.util.Collections
import kotlinx.coroutines.runBlocking

const val STATUS_NOT_INSTALLED = 8
const val CANCELED = 6
const val STATUS_FAILED = 5
const val STATUS_COMPLETED = 4
const val STATUS_DOWNLOADING = 2
const val STATUS_INITIAL_STATE = 1

const val ERROR_CODE_SUCCESS = 0
const val ERROR_CODE_FAIL = -5

const val KEY_ERROR_CODE = "error_code"
const val KEY_MODULE_NAME = "module_name"
const val KEY_RESOURCE_PACKAGE_NAME = "resourcePackageName"

const val KEY_SESSION_ID = "session_id"
const val KEY_STATUS = "status"
const val KEY_PACK_VERSION = "pack_version"
const val KEY_PACK_BASE_VERSION = "pack_base_version"
const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
const val KEY_TOTAL_BYTES_TO_DOWNLOAD = "total_bytes_to_download"
const val KEY_PACK_NAMES = "pack_names"
const val KEY_APP_VERSION_CODE = "app_version_code"
const val KEY_INSTALLED_ASSET_MODULE = "installed_asset_module"

const val KEY_CACHE_DIR = "CacheDir"
const val KEY_INDEX = "index"
const val KEY_CHUNK_NAME = "chunkName"
const val KEY_CHUNK_INTENT = "chunk_intents"
const val KEY_CHUNK_NUMBER = "chunk_number"
const val KEY_CHUNK_FILE_DESCRIPTOR = "chunk_file_descriptor"
const val KEY_RESOURCE_LINK = "resourceLink"
const val KEY_BYTE_LENGTH = "byteLength"
const val KEY_RESOURCE_BLOCK_NAME = "resourceBlockName"
const val KEY_UNCOMPRESSED_SIZE = "uncompressed_size"
const val KEY_UNCOMPRESSED_HASH_SHA256 = "uncompressed_hash_sha256"
const val KEY_NUMBER_OF_SUBCONTRACTORS = "numberOfSubcontractors"

const val KEY_USING_EXTRACTOR_STREAM = "usingExtractorStream"
const val KEY_COMPRESSION_FORMAT = "compression_format"
const val KEY_SLICE_IDS = "slice_ids"
const val KEY_SLICE_ID = "slice_id"
const val KEY_PLAY_CORE_VERSION_CODE = "playcore_version_code"

const val ACTION_VIEW = "android.intent.action.VIEW"
const val TAG_REQUEST = "asset_module"

private const val ACTION_SESSION_UPDATE = "com.google.android.play.core.assetpacks.receiver.ACTION_SESSION_UPDATE"
private const val EXTRA_SESSION_STATE = "com.google.android.play.core.assetpacks.receiver.EXTRA_SESSION_STATE"
private const val FLAGS = "com.google.android.play.core.FLAGS"

private const val ASSET_MODULE_DELIVERY_URL = "https://play-fe.googleapis.com/fdfe/assetModuleDelivery"
private const val SYNC_NOCACHE_QOS = "https://play-fe.googleapis.com/fdfe/sync?nocache_qos=lt"

private const val TAG = "AssetModuleRequest"

fun combineModule(key: String, vararg moduleNames: String?): String {
    return moduleNames.joinToString(separator = ":", prefix = "$key:")
}

fun getAppVersionCode(context: Context, packageName: String): String? {
    return runCatching { context.packageManager.getPackageInfo(packageName, 0).versionCode.toString() }.getOrNull()
}

fun HttpClient.initAssertModuleData(
    context: Context,
    packageName: String,
    accountManager: AccountManager,
    requestedAssetModuleNames: List<String?>,
    playCoreVersionCode: Int,
): DownloadData {
    val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
    var oauthToken: String? = null
    if (accounts.isEmpty()) {
        return DownloadData(errorCode = ERROR_CODE_FAIL)
    } else {
        for (account: Account in accounts) {
            oauthToken = runBlocking {
                accountManager.getAuthToken(account, AUTH_TOKEN_SCOPE, false).getString(AccountManager.KEY_AUTHTOKEN)
            }
            if (oauthToken != null) {
                break
            }
        }
    }

    if (oauthToken == null) {
        return DownloadData(errorCode = ERROR_CODE_FAIL)
    }

    val requestPayload = AssetModuleDeliveryRequest.Builder()
        .callerInfo(CallerInfo(getAppVersionCode(context, packageName)?.toInt()))
        .packageName(packageName)
        .playCoreVersion(playCoreVersionCode)
        .pageSource(listOf(PageSource.UNKNOWN_SEARCH_TRAFFIC_SOURCE, PageSource.BOOKS_HOME_PAGE))
        .callerState(listOf(CallerState.CALLER_APP_REQUEST, CallerState.CALLER_APP_DEBUGGABLE))
        .moduleInfo(ArrayList<AssetModuleInfo>().apply {
            requestedAssetModuleNames.forEach { add(AssetModuleInfo.Builder().name(it).build()) }
        }).build()

    val androidId = GServices.getString(context.contentResolver, "android_id", "0")?.toLong() ?: 1

    var moduleDeliveryInfo = runBlocking {
        runCatching {
            post(
                url = ASSET_MODULE_DELIVERY_URL,
                headers = getLicenseRequestHeaders(oauthToken, androidId),
                payload = requestPayload,
                adapter = AssetModuleDeliveryResponse.ADAPTER
            ).wrapper?.deliveryInfo
        }.getOrNull()
    }

    if (moduleDeliveryInfo?.status != 2) {
        return initModuleDownloadInfo(context, packageName, moduleDeliveryInfo)
    }

    runBlocking {
        runCatching {
            post(
                url = SYNC_NOCACHE_QOS,
                headers = getLicenseRequestHeaders(oauthToken, androidId),
                payload = DeviceSyncInfo.buildSyncRequest(context, androidId.toString(), accounts.first()),
                adapter = SyncResponse.ADAPTER
            )
        }.onFailure {
            Log.d(TAG, "initAssertModuleData: ", it)
        }
    }

    moduleDeliveryInfo = runBlocking {
        runCatching {
            post(
                url = ASSET_MODULE_DELIVERY_URL,
                headers = getLicenseRequestHeaders(oauthToken, androidId),
                payload = requestPayload,
                adapter = AssetModuleDeliveryResponse.ADAPTER
            ).wrapper?.deliveryInfo
        }.getOrNull()
    }
    Log.d(TAG, "initAssertModuleData: moduleDeliveryInfo-> $moduleDeliveryInfo")
    return initModuleDownloadInfo(context, packageName, moduleDeliveryInfo)
}

fun initModuleDownloadInfo(context: Context, packageName: String, deliveryInfo: ModuleDeliveryInfo?): DownloadData {
    if (deliveryInfo == null || deliveryInfo.status != null) {
        return DownloadData(errorCode = ERROR_CODE_FAIL)
    }
    val packNames: ArraySet<String> = arraySetOf()
    var moduleDownloadByteLength = 0L
    var appVersionCode = 0L
    val sessionIds = arrayMapOf<String, Int>()
    val moduleDataList = arrayMapOf<String, ModuleData>()
    for (deliveryIndex in deliveryInfo.res.indices) {
        val resource: ModuleResource = deliveryInfo.res[deliveryIndex]
        appVersionCode = resource.versionCode ?: 0
        val resourceList: List<PackResource> = resource.packResource
        val resourcePackageName: String = resource.packName ?: continue
        var packDownloadByteLength = 0L
        packNames.add(resourcePackageName)
        sessionIds[resourcePackageName] = deliveryIndex + 10
        var totalSumOfSubcontractedModules = 0
        val listOfSubcontractNames: ArrayList<String> = ArrayList()
        val dataBundle: ArrayList<Bundle> = arrayListOf()
        for (resIndex in resourceList.indices) {
            val packResource: PackResource = resourceList[resIndex]
            if (packResource.downloadInfo == null || packResource.chunkInfo == null) {
                continue
            }
            val downloadList = packResource.downloadInfo.download
            val numberOfSubcontractors = downloadList.size
            val uncompressedSize = packResource.downloadInfo.uncompressedSize
            val uncompressedHashSha256 = packResource.downloadInfo.uncompressedHashCode
            val chunkName = packResource.chunkInfo.chunkName?.also { listOfSubcontractNames.add(it) }
            var resDownloadByteLength = 0L
            for (downIndex in downloadList.indices) {
                val dResource: Download = downloadList[downIndex]
                resDownloadByteLength += dResource.byteLength!!
                totalSumOfSubcontractedModules += 1
                val bundle = Bundle()
                bundle.putString(KEY_CACHE_DIR, context.cacheDir.toString())
                bundle.putInt(KEY_INDEX, deliveryIndex + 10)
                bundle.putString(KEY_RESOURCE_PACKAGE_NAME, resourcePackageName)
                bundle.putString(KEY_CHUNK_NAME, chunkName)
                bundle.putString(KEY_RESOURCE_LINK, dResource.resourceLink)
                bundle.putLong(KEY_BYTE_LENGTH, dResource.byteLength)
                bundle.putString(KEY_RESOURCE_BLOCK_NAME, downIndex.toString())
                bundle.putLong(KEY_UNCOMPRESSED_SIZE, uncompressedSize ?: 0)
                bundle.putString(KEY_UNCOMPRESSED_HASH_SHA256, uncompressedHashSha256)
                bundle.putInt(KEY_NUMBER_OF_SUBCONTRACTORS, numberOfSubcontractors)
                dataBundle.add(bundle)
            }
            packDownloadByteLength += resDownloadByteLength
        }
        val moduleData = ModuleData(appVersionCode = appVersionCode, moduleVersion = 0, sessionId = STATUS_NOT_INSTALLED, errorCode = ERROR_CODE_SUCCESS, status = STATUS_NOT_INSTALLED, bytesDownloaded = 0, totalBytesToDownload = packDownloadByteLength, packBundleList = dataBundle, listOfSubcontractNames = listOfSubcontractNames)
        moduleDownloadByteLength += packDownloadByteLength
        moduleDataList[resourcePackageName] = moduleData
    }
    return DownloadData(packageName = packageName, errorCode = ERROR_CODE_SUCCESS, sessionIds = sessionIds, bytesDownloaded = 0, status = STATUS_NOT_INSTALLED, moduleNames = packNames, appVersionCode = appVersionCode, totalBytesToDownload = moduleDownloadByteLength, moduleDataList)
}

fun buildDownloadBundle(downloadData: DownloadData, list: List<Bundle?>? = null): Bundle {
    val bundleData = Bundle()
    val arrayList = arrayListOf<String>()
    var totalBytesToDownload = 0L
    var bytesDownloaded = 0L

    list?.forEach {
        val moduleName = it?.getString(KEY_MODULE_NAME)
        val packData = downloadData.getModuleData(moduleName!!)
        bundleData.putInt(KEY_STATUS, packData.status)
        downloadData.sessionIds[moduleName]?.let { sessionId -> bundleData.putInt(KEY_SESSION_ID, sessionId) }
        bundleData.putInt(combineModule(KEY_SESSION_ID, moduleName), packData.sessionId)
        bundleData.putInt(combineModule(KEY_STATUS, moduleName), packData.status)
        bundleData.putInt(combineModule(KEY_ERROR_CODE, moduleName), packData.errorCode)
        bundleData.putInt(combineModule(KEY_PACK_VERSION, moduleName), packData.appVersionCode.toInt())
        bundleData.putLong(combineModule(KEY_PACK_BASE_VERSION, moduleName), packData.moduleVersion)
        bundleData.putLong(combineModule(KEY_BYTES_DOWNLOADED, moduleName), packData.bytesDownloaded)
        bundleData.putLong(combineModule(KEY_TOTAL_BYTES_TO_DOWNLOAD, moduleName), packData.totalBytesToDownload)
        arrayList.add(moduleName)
        totalBytesToDownload += packData.totalBytesToDownload
        bytesDownloaded += packData.bytesDownloaded
    }
    bundleData.putStringArrayList(KEY_PACK_NAMES, arrayList)
    bundleData.putLong(KEY_TOTAL_BYTES_TO_DOWNLOAD, totalBytesToDownload)
    bundleData.putLong(KEY_BYTES_DOWNLOADED, bytesDownloaded)
    return bundleData
}

fun sendBroadcastForExistingFile(context: Context, downloadData: DownloadData, moduleName: String, bundle: Bundle?, destination: File?) {
    val packData = downloadData.getModuleData(moduleName)
    try {
        val downloadBundle = Bundle()
        downloadBundle.putInt(KEY_APP_VERSION_CODE, downloadData.appVersionCode.toInt())
        downloadBundle.putInt(KEY_ERROR_CODE, ERROR_CODE_SUCCESS)
        downloadBundle.putInt(KEY_SESSION_ID, downloadData.sessionIds[moduleName]
                ?: downloadData.status)
        downloadBundle.putInt(KEY_STATUS, packData.status)
        downloadBundle.putStringArrayList(KEY_PACK_NAMES, arrayListOf(moduleName))
        downloadBundle.putLong(KEY_BYTES_DOWNLOADED, packData.bytesDownloaded)
        downloadBundle.putLong(KEY_TOTAL_BYTES_TO_DOWNLOAD, packData.totalBytesToDownload)
        downloadBundle.putLong(combineModule(KEY_TOTAL_BYTES_TO_DOWNLOAD, moduleName), packData.totalBytesToDownload)
        downloadBundle.putLong(combineModule(KEY_PACK_VERSION, moduleName), packData.appVersionCode)
        downloadBundle.putInt(combineModule(KEY_STATUS, moduleName), packData.status)
        downloadBundle.putInt(combineModule(KEY_ERROR_CODE, moduleName), ERROR_CODE_SUCCESS)
        downloadBundle.putLong(combineModule(KEY_BYTES_DOWNLOADED, moduleName), packData.bytesDownloaded)
        downloadBundle.putLong(combineModule(KEY_PACK_BASE_VERSION, moduleName), packData.moduleVersion)
        val resultList = arraySetOf<Bundle>()
        packData.packBundleList.forEach {
            val result = Bundle()
            result.putString(KEY_CHUNK_NAME, it.getString(KEY_CHUNK_NAME))
            result.putLong(KEY_UNCOMPRESSED_SIZE, it.getLong(KEY_UNCOMPRESSED_SIZE))
            result.putString(KEY_UNCOMPRESSED_HASH_SHA256, it.getString(KEY_UNCOMPRESSED_HASH_SHA256))
            result.putInt(KEY_NUMBER_OF_SUBCONTRACTORS, it.getInt(KEY_NUMBER_OF_SUBCONTRACTORS))
            result.putLong(KEY_BYTE_LENGTH, it.getLong(KEY_BYTE_LENGTH))
            resultList.add(result)
        }
        resultList.forEach {
            val chunkName = it.getString(KEY_CHUNK_NAME)
            val uncompressedSize = it.getLong(KEY_UNCOMPRESSED_SIZE)
            val uncompressedHashSha256 = it.getString(KEY_UNCOMPRESSED_HASH_SHA256)
            val numberOfSubcontractors = it.getInt(KEY_NUMBER_OF_SUBCONTRACTORS)
            val chunkIntents: ArrayList<Intent?>
            if (destination == null) {
                chunkIntents = ArrayList(Collections.nCopies<Intent?>(numberOfSubcontractors, null))
            } else {
                val uFile = Uri.parse(destination.absolutePath).path?.let { path -> File(path) }
                chunkIntents = ArrayList(Collections.nCopies<Intent?>(numberOfSubcontractors, null))
                val uri = Uri.fromFile(uFile)
                context.grantUriPermission(moduleName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val intent = Intent(ACTION_VIEW)
                intent.setDataAndType(uri, context.contentResolver.getType(uri))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val resourceBlockIndex = bundle?.getString(KEY_RESOURCE_BLOCK_NAME)?.toInt()
                if (uFile?.exists() == true && bundle?.getString(KEY_CHUNK_NAME) == chunkName && resourceBlockIndex != null) {
                    if (chunkIntents[resourceBlockIndex] == null) {
                        chunkIntents[resourceBlockIndex] = intent
                    }
                }
            }
            downloadBundle.putParcelableArrayList(combineModule(KEY_CHUNK_INTENT, moduleName, chunkName), chunkIntents)
            downloadBundle.putLong(combineModule(KEY_UNCOMPRESSED_SIZE, moduleName, chunkName), uncompressedSize)
            downloadBundle.putInt(combineModule(KEY_COMPRESSION_FORMAT, moduleName, chunkName), 1)
            downloadBundle.putString(combineModule(KEY_UNCOMPRESSED_HASH_SHA256, moduleName, chunkName), uncompressedHashSha256)
        }
        downloadBundle.putStringArrayList(combineModule(KEY_SLICE_IDS, moduleName), packData.listOfSubcontractNames)
        sendBroadCast(context, downloadData, downloadBundle)
    } catch (e: Exception) {
        Log.w(TAG, "sendBroadcastForExistingFile error:" + e.message)
    }
}

private fun sendBroadCast(context: Context, downloadData: DownloadData, result: Bundle) {
    val intent = Intent()
    intent.setAction(ACTION_SESSION_UPDATE)
    intent.putExtra(EXTRA_SESSION_STATE, result)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    intent.putExtra(FLAGS, Bundle().apply { putBoolean(KEY_USING_EXTRACTOR_STREAM, true) })
    intent.setPackage(downloadData.packageName)
    context.sendBroadcast(intent)
}
