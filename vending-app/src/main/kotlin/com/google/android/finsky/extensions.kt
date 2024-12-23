/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.collection.ArraySet
import androidx.collection.arrayMapOf
import androidx.collection.arraySetOf
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.AUTH_TOKEN_SCOPE
import com.android.vending.VendingPreferences
import com.android.vending.getRequestHeaders
import com.android.vending.licensing.getAuthToken
import com.google.android.finsky.assetmoduleservice.AssetPackException
import com.google.android.finsky.assetmoduleservice.ChunkData
import com.google.android.finsky.assetmoduleservice.DownloadData
import com.google.android.finsky.assetmoduleservice.ModuleData
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.assetpacks.protocol.BroadcastConstants
import com.google.android.play.core.assetpacks.protocol.BundleKeys
import com.google.android.play.core.assetpacks.protocol.CompressionFormat
import com.google.android.play.core.assetpacks.protocol.PatchFormat
import org.microg.gms.auth.AuthConstants
import org.microg.vending.billing.GServices
import org.microg.vending.billing.core.GooglePlayApi
import org.microg.vending.billing.core.HttpClient
import java.io.File
import java.util.Collections

const val KEY_MODULE_NAME = "module_name"

const val TAG_REQUEST = "asset_module"

private const val ASSET_MODULE_DELIVERY_URL = "https://play-fe.googleapis.com/fdfe/assetModuleDelivery"

private const val TAG = "AssetModuleRequest"

fun getAppVersionCode(context: Context, packageName: String): Long? {
    return try {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(packageName, 0))
    } catch (e: PackageManager.NameNotFoundException) {
        throw AssetPackException(AssetPackErrorCode.APP_UNAVAILABLE, e.message)
    }
}

fun <T> Bundle?.get(key: BundleKeys.RootKey<T>): T? = if (this == null) null else BundleKeys.get(this, key)
fun <T> Bundle?.get(key: BundleKeys.RootKey<T>, def: T): T = if (this == null) def else BundleKeys.get(this, key, def)
fun <T> Bundle.put(key: BundleKeys.RootKey<T>, v: T) = BundleKeys.put(this, key, v)
fun <T> Bundle.put(key: BundleKeys.PackKey<T>, packName: String, v: T) = BundleKeys.put(this, key, packName, v)
fun <T> Bundle.put(key: BundleKeys.SliceKey<T>, packName: String, sliceId: String, v: T) = BundleKeys.put(this, key, packName, sliceId, v)
fun <T> bundleOf(pair: Pair<BundleKeys.RootKey<T>, T>): Bundle = Bundle().apply { put(pair.first, pair.second) }
operator fun Bundle.plus(other: Bundle): Bundle = Bundle(this).apply { putAll(other) }
operator fun <T> Bundle.plus(pair: Pair<BundleKeys.RootKey<T>, T>): Bundle = this + bundleOf(pair)

val Context.assetPacksDir: File
    get() = File(filesDir, "assetpacks")
fun Context.getSessionDir(sessionId: Int) =
    File(assetPacksDir, sessionId.toString())
fun Context.getModuleDir(sessionId: Int, moduleName: String): File =
    File(getSessionDir(sessionId), moduleName)
fun Context.getSliceDir(sessionId: Int, moduleName: String, sliceId: String) =
    File(getModuleDir(sessionId, moduleName), sliceId)
fun Context.getChunkFile(sessionId: Int, moduleName: String, sliceId: String, chunkNumber: Int): File =
    File(getSliceDir(sessionId, moduleName, sliceId), chunkNumber.toString())

data class AssetModuleOptions(val playCoreVersionCode: Int, val supportedCompressionFormats: List<Int>, val supportedPatchFormats: List<Int>)

suspend fun HttpClient.initAssetModuleData(
    context: Context,
    packageName: String,
    accountManager: AccountManager,
    requestedAssetModuleNames: List<String?>,
    options: AssetModuleOptions,
    playCoreVersionCode: Int = options.playCoreVersionCode,
    supportedCompressionFormats: List<Int> = options.supportedCompressionFormats.takeIf { it.isNotEmpty() } ?: listOf(CompressionFormat.UNSPECIFIED, CompressionFormat.CHUNKED_GZIP),
    supportedPatchFormats: List<Int> = options.supportedPatchFormats.takeIf { it.isNotEmpty() } ?: listOf(PatchFormat.PATCH_GDIFF, PatchFormat.GZIPPED_GDIFF),
): DownloadData? {
    val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
    var authToken: String? = null
    var account: Account? = null
    if (accounts.isEmpty()) {
        return null
    } else {
        for (candidate in accounts) {
            authToken = accountManager.getAuthToken(candidate, AUTH_TOKEN_SCOPE, false).getString(AccountManager.KEY_AUTHTOKEN)
            if (authToken != null) {
                account = candidate
                break
            }
        }
    }

    if (authToken == null || account == null) {
        return null
    }

    val appVersionCode = getAppVersionCode(context, packageName)
    val requestPayload = AssetModuleDeliveryRequest.Builder().callerInfo(CallerInfo(appVersionCode)).packageName(packageName)
        .playCoreVersion(playCoreVersionCode).supportedCompressionFormats(supportedCompressionFormats)
        .supportedPatchFormats(supportedPatchFormats).modules(ArrayList<AssetModuleInfo>().apply {
            requestedAssetModuleNames.forEach { add(AssetModuleInfo.Builder().name(it).build()) }
        }).build()

    val androidId = GServices.getString(context.contentResolver, "android_id", "1")?.toLong() ?: 1
    // Make sure device is synced
    syncDeviceInfo(context, account, authToken, androidId)

    val moduleDeliveryInfo = post(
        url = ASSET_MODULE_DELIVERY_URL,
        headers = getRequestHeaders(authToken, androidId),
        payload = requestPayload,
        adapter = AssetModuleDeliveryResponse.ADAPTER
    ).wrapper?.deliveryInfo
    Log.d(TAG, "initAssetModuleData: moduleDeliveryInfo-> $moduleDeliveryInfo")
    return initModuleDownloadInfo(packageName, appVersionCode, moduleDeliveryInfo)
}

const val DEVICE_INFO_SYNC_INTERVAL = 24 * 60 * 60 * 1000L // 1 day
suspend fun syncDeviceInfo(context: Context, account: Account, authToken: String, androidId: Long) {
    val deviceSyncEnabled = VendingPreferences.isDeviceSyncEnabled(context)
    if (!deviceSyncEnabled) {
        Log.d(TAG, "syncDeviceInfo deviceSyncEnabled is false")
        return
    }
    val prefs = context.getSharedPreferences("device_sync", Context.MODE_PRIVATE)
    val lastSync = prefs.getLong("${account.name}_last", 0)
    if (lastSync > System.currentTimeMillis() - DEVICE_INFO_SYNC_INTERVAL) {
        return
    }
    runCatching {
        HttpClient(context).post(
            url = GooglePlayApi.URL_SYNC,
            headers = getRequestHeaders(authToken, androidId),
            payload = DeviceSyncInfo.buildSyncRequest(context, androidId, account),
            adapter = SyncResponse.ADAPTER
        )
        prefs.edit().putLong("${account.name}_last", System.currentTimeMillis()).apply()
        Log.d(TAG, "syncDeviceInfo: sync success")
    }.onFailure {
        Log.d(TAG, "syncDeviceInfo: sync error", it)
    }
}

private val sessionIdMap: MutableMap<String, Int> = mutableMapOf()

private val lock = Any()
private fun Context.generateSessionId(): Int {
    synchronized(lock) {
        val sharedPreferences = getSharedPreferences("AssetModuleSessionIdGenerator", 0)
        val latest = sharedPreferences.getInt("Latest", 0) + 1
        val edit = sharedPreferences.edit()
        edit.putInt("Latest", latest)
        edit.commit()
        return latest
    }
}

private fun getSessionIdForPackage(packageName: String): Int {
    synchronized(lock) {
        return sessionIdMap.getOrPut(packageName) { 10 }
    }
}

private fun updateSessionIdForPackage(packageName: String, increment: Int) {
    synchronized(lock) {
        val currentSessionId = sessionIdMap[packageName] ?: 10
        sessionIdMap[packageName] = currentSessionId + increment
    }
}

private fun initModuleDownloadInfo(packageName: String, appVersionCode: Long?, deliveryInfo: ModuleDeliveryInfo?): DownloadData? {
    if (deliveryInfo == null || deliveryInfo.status != null) {
        return null
    }
    val moduleNames: ArraySet<String> = arraySetOf()
    var totalBytesToDownload = 0L
    var packVersionCode = 0L
    val sessionIds = arrayMapOf<String, Int>()
    val moduleDataMap = arrayMapOf<String, ModuleData>()
    val baseSessionId = getSessionIdForPackage(packageName)
    for (moduleIndex in deliveryInfo.modules.indices) {
        val moduleInfo: ModuleInfo = deliveryInfo.modules[moduleIndex]
        packVersionCode = moduleInfo.packVersion ?: 0
        val slices: List<SliceInfo> = moduleInfo.slices
        val moduleName: String = moduleInfo.moduleName ?: continue
        var moduleBytesToDownload = 0L
        moduleNames.add(moduleName)
        sessionIds[moduleName] = baseSessionId + moduleIndex
        var totalSumOfSubcontractedModules = 0
        val sliceIds: ArrayList<String> = ArrayList()
        val chunkDatas: ArrayList<ChunkData> = arrayListOf()
        for (sliceIndex in slices.indices) {
            val sliceInfo: SliceInfo = slices[sliceIndex]
            if (sliceInfo.metadata == null || sliceInfo.fullDownloadInfo == null) {
                continue
            }
            val chunks = sliceInfo.fullDownloadInfo.chunks
            val numberOfChunks = chunks.size
            val uncompressedSize = sliceInfo.fullDownloadInfo.uncompressedSize
            val uncompressedHashSha256 = sliceInfo.fullDownloadInfo.uncompressedHashSha256
            val sliceId = sliceInfo.metadata.sliceId?.also { sliceIds.add(it) } ?: continue
            var sliceBytesToDownload = 0L
            for (chunkIndex in chunks.indices) {
                val dResource: ChunkInfo = chunks[chunkIndex]
                sliceBytesToDownload += dResource.bytesToDownload!!
                totalSumOfSubcontractedModules += 1
                chunkDatas.add(ChunkData(
                    sessionId = sessionIds[moduleName]!!,
                    moduleName = moduleName,
                    sliceId = sliceId,
                    chunkSourceUri = dResource.sourceUri,
                    chunkBytesToDownload = dResource.bytesToDownload,
                    chunkIndex = chunkIndex,
                    sliceCompressionFormat = sliceInfo.fullDownloadInfo.compressionFormat ?: CompressionFormat.UNSPECIFIED,
                    sliceUncompressedSize = uncompressedSize ?: 0,
                    sliceUncompressedHashSha256 = uncompressedHashSha256,
                    numberOfChunksInSlice = numberOfChunks
                ))
            }
            moduleBytesToDownload += sliceBytesToDownload
        }
        val moduleData = ModuleData(
            packVersionCode = packVersionCode,
            moduleVersion = 0,
            errorCode = AssetPackErrorCode.NO_ERROR,
            status = AssetPackStatus.NOT_INSTALLED,
            bytesDownloaded = 0,
            totalBytesToDownload = moduleBytesToDownload,
            chunks = chunkDatas,
            sliceIds = sliceIds
        )
        totalBytesToDownload += moduleBytesToDownload
        moduleDataMap[moduleName] = moduleData
    }
    updateSessionIdForPackage(packageName, deliveryInfo.modules.size)
    return DownloadData(
        packageName = packageName,
        errorCode = AssetPackErrorCode.NO_ERROR,
        sessionIds = sessionIds,
        status = AssetPackStatus.NOT_INSTALLED,
        moduleNames = moduleNames,
        appVersionCode = appVersionCode ?: packVersionCode,
        moduleDataMap
    )
}

fun buildDownloadBundle(downloadData: DownloadData, list: List<String>? = null): Bundle {
    val bundleData = Bundle()
    val arrayList = arrayListOf<String>()
    var totalBytesToDownload = 0L
    var bytesDownloaded = 0L

    list?.forEach { moduleName ->
        val packData = downloadData.getModuleData(moduleName)
        bundleData.put(BundleKeys.STATUS, packData.status)
        downloadData.sessionIds[moduleName]?.let { sessionId ->
            bundleData.put(BundleKeys.SESSION_ID, sessionId)
            bundleData.put(BundleKeys.SESSION_ID, moduleName, packData.status)
        }
        bundleData.put(BundleKeys.PACK_VERSION_TAG, moduleName, null)
        bundleData.put(BundleKeys.STATUS, moduleName, packData.status)
        bundleData.put(BundleKeys.ERROR_CODE, moduleName, packData.errorCode)
        bundleData.put(BundleKeys.PACK_VERSION, moduleName, packData.packVersionCode)
        bundleData.put(BundleKeys.PACK_BASE_VERSION, moduleName, packData.moduleVersion)
        bundleData.put(BundleKeys.BYTES_DOWNLOADED, moduleName, packData.bytesDownloaded)
        bundleData.put(BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, moduleName, packData.totalBytesToDownload)
        arrayList.add(moduleName)
        totalBytesToDownload += packData.totalBytesToDownload
        bytesDownloaded += packData.bytesDownloaded
    }
    bundleData.put(BundleKeys.ERROR_CODE, downloadData.errorCode)
    bundleData.put(BundleKeys.PACK_NAMES, arrayList)
    bundleData.put(BundleKeys.TOTAL_BYTES_TO_DOWNLOAD, totalBytesToDownload)
    bundleData.put(BundleKeys.BYTES_DOWNLOADED, bytesDownloaded)
    return bundleData
}

fun sendBroadcastForExistingFile(context: Context, downloadData: DownloadData, moduleName: String, chunkData: ChunkData?, destination: File?): Bundle {
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
        downloadBundle.put(BundleKeys.PACK_VERSION, moduleName, packData.packVersionCode)
        downloadBundle.put(BundleKeys.STATUS, moduleName, packData.status)
        downloadBundle.put(BundleKeys.ERROR_CODE, moduleName, AssetPackErrorCode.NO_ERROR)
        downloadBundle.put(BundleKeys.BYTES_DOWNLOADED, moduleName, packData.bytesDownloaded)
        downloadBundle.put(BundleKeys.PACK_BASE_VERSION, moduleName, packData.moduleVersion)
        downloadBundle.put(BundleKeys.PACK_VERSION_TAG, moduleName, null)
        packData.chunks.map { it.copy() }.forEach {
            val sliceId = it.sliceId
            val chunkIntents = ArrayList(Collections.nCopies<Intent?>(it.numberOfChunksInSlice, null))
            if (chunkData != null && destination != null) {
                val uri = Uri.fromFile(destination)
                context.grantUriPermission(moduleName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, context.contentResolver.getType(uri))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (destination.exists() && chunkData.moduleName == moduleName && chunkData.sliceId == sliceId) {
                    if (chunkIntents[chunkData.chunkIndex] == null) {
                        chunkIntents[chunkData.chunkIndex] = intent
                    }
                }
            }
            downloadBundle.put(BundleKeys.CHUNK_INTENTS, moduleName, sliceId, chunkIntents)
            downloadBundle.put(BundleKeys.UNCOMPRESSED_SIZE, moduleName, sliceId, it.sliceUncompressedSize)
            downloadBundle.put(BundleKeys.COMPRESSION_FORMAT, moduleName, sliceId, it.sliceCompressionFormat)
            downloadBundle.put(BundleKeys.UNCOMPRESSED_HASH_SHA256, moduleName, sliceId, it.sliceUncompressedHashSha256)
        }
        downloadBundle.put(BundleKeys.SLICE_IDS, moduleName, ArrayList(packData.chunks.map { it.sliceId }.distinct()))
        sendBroadCast(context, downloadData, downloadBundle)
        return downloadBundle
    } catch (e: Exception) {
        Log.w(TAG, "sendBroadcastForExistingFile error:" + e.message)
        return Bundle(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
    }
}

private fun sendBroadCast(context: Context, downloadData: DownloadData, result: Bundle) {
    val intent = Intent()
    intent.setAction(BroadcastConstants.ACTION_SESSION_UPDATE)
    intent.putExtra(BroadcastConstants.EXTRA_SESSION_STATE, result)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    intent.putExtra(BroadcastConstants.EXTRA_FLAGS, Bundle().apply { putBoolean(BroadcastConstants.KEY_USING_EXTRACTOR_STREAM, true) })
    intent.setPackage(downloadData.packageName)
    context.sendBroadcast(intent)
}
