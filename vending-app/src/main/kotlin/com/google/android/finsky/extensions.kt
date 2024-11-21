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
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.assetpacks.protocol.BroadcastConstants
import com.google.android.play.core.assetpacks.protocol.BundleKeys
import kotlinx.coroutines.runBlocking
import org.microg.gms.auth.AuthConstants
import org.microg.vending.billing.GServices
import org.microg.vending.billing.core.HttpClient
import java.io.File
import java.util.Collections

const val KEY_ERROR_CODE = "error_code"
const val KEY_MODULE_NAME = "module_name"
const val KEY_RESOURCE_PACKAGE_NAME = "resourcePackageName"

const val KEY_CACHE_DIR = "CacheDir"
const val KEY_INDEX = "index"
const val KEY_CHUNK_NAME = "chunkName"
const val KEY_RESOURCE_LINK = "resourceLink"
const val KEY_BYTE_LENGTH = "byteLength"
const val KEY_RESOURCE_BLOCK_NAME = "resourceBlockName"
const val KEY_UNCOMPRESSED_SIZE = "uncompressed_size"
const val KEY_UNCOMPRESSED_HASH_SHA256 = "uncompressed_hash_sha256"
const val KEY_NUMBER_OF_SUBCONTRACTORS = "numberOfSubcontractors"

const val KEY_USING_EXTRACTOR_STREAM = "usingExtractorStream"

const val ACTION_VIEW = "android.intent.action.VIEW"
const val TAG_REQUEST = "asset_module"

private const val FLAGS = "com.google.android.play.core.FLAGS"

private const val ASSET_MODULE_DELIVERY_URL = "https://play-fe.googleapis.com/fdfe/assetModuleDelivery"

private const val TAG = "AssetModuleRequest"

fun getAppVersionCode(context: Context, packageName: String): String? {
    return runCatching { context.packageManager.getPackageInfo(packageName, 0).versionCode.toString() }.getOrNull()
}

fun <T> Bundle?.get(key: BundleKeys.RootKey<T>): T? = if (this == null) null else BundleKeys.get(this, key)
fun <T> Bundle?.get(key: BundleKeys.RootKey<T>, def: T): T = if (this == null) def else BundleKeys.get(this, key, def)
fun <T> Bundle.put(key: BundleKeys.RootKey<T>, v: T) = BundleKeys.put(this, key, v)
fun <T> Bundle.put(key: BundleKeys.PackKey<T>, packName: String, v: T) = BundleKeys.put(this, key, packName, v)
fun <T> Bundle.put(key: BundleKeys.SliceKey<T>, packName: String, sliceId: String, v: T) = BundleKeys.put(this, key, packName, sliceId, v)
fun <T> bundleOf(pair: Pair<BundleKeys.RootKey<T>, T>): Bundle = Bundle().apply { put(pair.first, pair.second) }


fun HttpClient.initAssertModuleData(
    context: Context,
    packageName: String,
    accountManager: AccountManager,
    requestedAssetModuleNames: List<String?>,
    playCoreVersionCode: Int,
    supportedCompressionFormats: List<Int> = listOf(0, 1),
    supportedPatchFormats: List<Int> = listOf(1, 2),
): DownloadData? {
    val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
    var oauthToken: String? = null
    if (accounts.isEmpty()) {
        return null
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
        return null
    }

    val requestPayload = AssetModuleDeliveryRequest.Builder().callerInfo(CallerInfo(getAppVersionCode(context, packageName)?.toInt())).packageName(packageName)
        .playCoreVersion(playCoreVersionCode).supportedCompressionFormats(supportedCompressionFormats)
        .supportedPatchFormats(supportedPatchFormats).modules(ArrayList<AssetModuleInfo>().apply {
            requestedAssetModuleNames.forEach { add(AssetModuleInfo.Builder().name(it).build()) }
        }).build()

    val androidId = GServices.getString(context.contentResolver, "android_id", "0")?.toLong() ?: 1

    val moduleDeliveryInfo = runBlocking {
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

private fun initModuleDownloadInfo(context: Context, packageName: String, deliveryInfo: ModuleDeliveryInfo?): DownloadData? {
    if (deliveryInfo == null || deliveryInfo.status != null) {
        return null
    }
    val moduleNames: ArraySet<String> = arraySetOf()
    var totalBytesToDownload = 0L
    var packVersionCode = 0L
    val sessionIds = arrayMapOf<String, Int>()
    val moduleDataMap = arrayMapOf<String, ModuleData>()
    for (moduleIndex in deliveryInfo.modules.indices) {
        val moduleInfo: ModuleInfo = deliveryInfo.modules[moduleIndex]
        packVersionCode = moduleInfo.packVersion ?: 0
        val slices: List<SliceInfo> = moduleInfo.slices
        val moduleName: String = moduleInfo.moduleName ?: continue
        var moduleBytesToDownload = 0L
        moduleNames.add(moduleName)
        sessionIds[moduleName] = moduleIndex + 10
        var totalSumOfSubcontractedModules = 0
        val sliceIds: ArrayList<String> = ArrayList()
        val dataBundle: ArrayList<Bundle> = arrayListOf()
        for (sliceIndex in slices.indices) {
            val sliceInfo: SliceInfo = slices[sliceIndex]
            if (sliceInfo.metadata == null || sliceInfo.fullDownloadInfo == null) {
                continue
            }
            val chunks = sliceInfo.fullDownloadInfo.chunks
            val chunkNumber = chunks.size
            val uncompressedSize = sliceInfo.fullDownloadInfo.uncompressedSize
            val uncompressedHashSha256 = sliceInfo.fullDownloadInfo.uncompressedHashSha256
            val chunkName = sliceInfo.metadata.sliceId?.also { sliceIds.add(it) }
            var sliceBytesToDownload = 0L
            for (chunkIndex in chunks.indices) {
                val dResource: ChunkInfo = chunks[chunkIndex]
                sliceBytesToDownload += dResource.bytesToDownload!!
                totalSumOfSubcontractedModules += 1
                val bundle = Bundle()
                bundle.putString(KEY_CACHE_DIR, context.cacheDir.toString())
                bundle.putInt(KEY_INDEX, moduleIndex + 10)
                bundle.putString(KEY_RESOURCE_PACKAGE_NAME, moduleName)
                bundle.putString(KEY_CHUNK_NAME, chunkName)
                bundle.putString(KEY_RESOURCE_LINK, dResource.sourceUri)
                bundle.putLong(KEY_BYTE_LENGTH, dResource.bytesToDownload)
                bundle.putString(KEY_RESOURCE_BLOCK_NAME, chunkIndex.toString())
                bundle.putLong(KEY_UNCOMPRESSED_SIZE, uncompressedSize ?: 0)
                bundle.putString(KEY_UNCOMPRESSED_HASH_SHA256, uncompressedHashSha256)
                bundle.putInt(KEY_NUMBER_OF_SUBCONTRACTORS, chunkNumber)
                dataBundle.add(bundle)
            }
            moduleBytesToDownload += sliceBytesToDownload
        }
        val moduleData = ModuleData(
            appVersionCode = packVersionCode,
            moduleVersion = 0,
            sessionId = AssetPackStatus.NOT_INSTALLED,
            errorCode = AssetPackErrorCode.NO_ERROR,
            status = AssetPackStatus.NOT_INSTALLED,
            bytesDownloaded = 0,
            totalBytesToDownload = moduleBytesToDownload,
            packBundleList = dataBundle,
            listOfSubcontractNames = sliceIds
        )
        totalBytesToDownload += moduleBytesToDownload
        moduleDataMap[moduleName] = moduleData
    }
    return DownloadData(
        packageName = packageName,
        errorCode = AssetPackErrorCode.NO_ERROR,
        sessionIds = sessionIds,
        bytesDownloaded = 0,
        status = AssetPackStatus.NOT_INSTALLED,
        moduleNames = moduleNames,
        appVersionCode = packVersionCode,
        totalBytesToDownload = totalBytesToDownload,
        moduleDataMap
    )
}

fun buildDownloadBundle(downloadData: DownloadData, list: List<Bundle?>? = null): Bundle {
    val bundleData = Bundle()
    val arrayList = arrayListOf<String>()
    var totalBytesToDownload = 0L
    var bytesDownloaded = 0L

    list?.forEach {
        val moduleName = it?.getString(KEY_MODULE_NAME)
        val packData = downloadData.getModuleData(moduleName!!)
        bundleData.put(BundleKeys.STATUS, packData.status)
        downloadData.sessionIds[moduleName]?.let { sessionId -> bundleData.put(BundleKeys.SESSION_ID, sessionId) }
        bundleData.put(BundleKeys.SESSION_ID, moduleName, packData.sessionId)
        bundleData.put(BundleKeys.STATUS, moduleName, packData.status)
        bundleData.put(BundleKeys.ERROR_CODE, moduleName, packData.errorCode)
        bundleData.put(BundleKeys.PACK_VERSION, moduleName, packData.appVersionCode)
        bundleData.put(BundleKeys.PACK_BASE_VERSION, moduleName, packData.moduleVersion)
        bundleData.put(BundleKeys.BYTES_DOWNLOADED, moduleName, packData.bytesDownloaded)
        bundleData.put(BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, moduleName, packData.totalBytesToDownload)
        arrayList.add(moduleName)
        totalBytesToDownload += packData.totalBytesToDownload
        bytesDownloaded += packData.bytesDownloaded
    }
    bundleData.put(BundleKeys.PACK_NAMES, arrayList)
    bundleData.put(BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, totalBytesToDownload)
    bundleData.put(BundleKeys.BYTES_DOWNLOADED, bytesDownloaded)
    return bundleData
}

fun sendBroadcastForExistingFile(context: Context, downloadData: DownloadData, moduleName: String, bundle: Bundle?, destination: File?): Bundle {
    val packData = downloadData.getModuleData(moduleName)
    try {
        val downloadBundle = Bundle()
        downloadBundle.put(BundleKeys.APP_VERSION_CODE, downloadData.appVersionCode.toInt())
        downloadBundle.put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NO_ERROR)
        downloadBundle.put(BundleKeys.SESSION_ID, downloadData.sessionIds[moduleName] ?: downloadData.status)
        downloadBundle.put(BundleKeys.STATUS, packData.status)
        downloadBundle.put(BundleKeys.PACK_NAMES, arrayListOf(moduleName))
        downloadBundle.put(BundleKeys.BYTES_DOWNLOADED, packData.bytesDownloaded)
        downloadBundle.put(BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, packData.totalBytesToDownload)
        downloadBundle.put(BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, moduleName, packData.totalBytesToDownload)
        downloadBundle.put(BundleKeys.PACK_VERSION, moduleName, packData.appVersionCode)
        downloadBundle.put(BundleKeys.STATUS, moduleName, packData.status)
        downloadBundle.put(BundleKeys.ERROR_CODE, moduleName, AssetPackErrorCode.NO_ERROR)
        downloadBundle.put(BundleKeys.BYTES_DOWNLOADED, moduleName, packData.bytesDownloaded)
        downloadBundle.put(BundleKeys.PACK_BASE_VERSION, moduleName, packData.moduleVersion)
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
            val sliceId = it.getString(KEY_CHUNK_NAME) ?: ""
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
                if (uFile?.exists() == true && bundle?.getString(KEY_CHUNK_NAME) == sliceId && resourceBlockIndex != null) {
                    if (chunkIntents[resourceBlockIndex] == null) {
                        chunkIntents[resourceBlockIndex] = intent
                    }
                }
            }
            downloadBundle.put(BundleKeys.CHUNK_INTENTS, moduleName, sliceId, chunkIntents)
            downloadBundle.put(BundleKeys.UNCOMPRESSED_SIZE, moduleName, sliceId, uncompressedSize)
            downloadBundle.put(BundleKeys.COMPRESSION_FORMAT, moduleName, sliceId, 1) // TODO
            downloadBundle.put(BundleKeys.UNCOMPRESSED_HASH_SHA256, moduleName, sliceId, uncompressedHashSha256)
        }
        downloadBundle.put(BundleKeys.SLICE_IDS, moduleName, packData.listOfSubcontractNames)
        sendBroadCast(context, downloadData, downloadBundle)
        return downloadBundle
    } catch (e: Exception) {
        Log.w(TAG, "sendBroadcastForExistingFile error:" + e.message)
        return Bundle(Bundle().apply { putInt(KEY_ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
    }
}

private fun sendBroadCast(context: Context, downloadData: DownloadData, result: Bundle) {
    val intent = Intent()
    intent.setAction(BroadcastConstants.ACTION_SESSION_UPDATE)
    intent.putExtra(BroadcastConstants.EXTRA_SESSION_STATE, result)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    intent.putExtra(FLAGS, Bundle().apply { putBoolean(KEY_USING_EXTRACTOR_STREAM, true) })
    intent.setPackage(downloadData.packageName)
    context.sendBroadcast(intent)
}
