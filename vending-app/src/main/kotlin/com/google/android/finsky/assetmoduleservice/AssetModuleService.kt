/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.finsky.assetmoduleservice

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.android.vending.VendingPreferences
import com.google.android.finsky.*
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.assetpacks.protocol.BundleKeys
import com.google.android.play.core.assetpacks.protocol.IAssetModuleService
import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback
import org.microg.gms.common.PackageUtils
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.core.HttpClient
import java.io.File

private const val TAG = "AssetModuleService"

class AssetModuleService : LifecycleService() {
    private lateinit var httpClient: HttpClient
    private lateinit var accountManager: AccountManager

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind: ")
        ProfileManager.ensureInitialized(this)
        accountManager = AccountManager.get(this)
        httpClient = HttpClient(this)
        return AssetModuleServiceImpl(this, lifecycle, httpClient, accountManager, packageDownloadData).asBinder()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        httpClient.requestQueue.cancelAll(TAG_REQUEST)
        super.onDestroy()
    }

    companion object {
        private val packageDownloadData = mutableMapOf<String, DownloadData?>()
    }
}

class AssetModuleServiceImpl(
    val context: Context, override val lifecycle: Lifecycle, private val httpClient: HttpClient, private val accountManager: AccountManager,
    private val packageDownloadData: MutableMap<String, DownloadData?>
) : IAssetModuleService.Stub(), LifecycleOwner {
    private val fileDescriptorMap = mutableMapOf<String, ParcelFileDescriptor>()

    private fun List<Bundle>.getModuleNames(): List<String> = mapNotNull { it.get(BundleKeys.MODULE_NAME) }
    private fun Bundle?.getInstalledAssetModules(): Map<String, Long> = get(BundleKeys.INSTALLED_ASSET_MODULE).orEmpty()
        .map { it.get(BundleKeys.INSTALLED_ASSET_MODULE_NAME) to it.get(BundleKeys.INSTALLED_ASSET_MODULE_VERSION) }
        .filter { it.first != null && it.second != null }
        .associate { it.first!! to it.second!! }

    private fun Bundle?.getOptions() = Options(
        this.get(BundleKeys.PLAY_CORE_VERSION_CODE, 0),
        this.get(BundleKeys.SUPPORTED_COMPRESSION_FORMATS).orEmpty(),
        this.get(BundleKeys.SUPPORTED_PATCH_FORMATS).orEmpty(),
    )

    private fun sendError(callback: IAssetModuleServiceCallback?, method: String, errorCode: @AssetPackErrorCode Int) {
        Log.w(TAG, "Sending error from $method: $errorCode")
        callback?.onError(bundleOf(BundleKeys.ERROR_CODE to errorCode))
    }

    override fun startDownload(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val moduleNames = list?.getModuleNames()
        val options = bundle.getOptions()
        val installedAssetModules = bundle.getInstalledAssetModules()

        Log.d(TAG, "startDownload[$packageName](moduleNames: $moduleNames, installedAssetModules: $installedAssetModules)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "startDownload", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || moduleNames.isNullOrEmpty()) {
            return sendError(callback, "startDownload", AssetPackErrorCode.INVALID_REQUEST)
        }

        if (packageDownloadData[packageName] == null ||
            packageDownloadData[packageName]?.packageName != packageName ||
            packageDownloadData[packageName]?.moduleNames?.intersect(moduleNames.toSet())?.isEmpty() == true) {
            packageDownloadData[packageName] = httpClient.initAssetModuleData(context, packageName, accountManager, moduleNames, options)
            if (packageDownloadData[packageName] == null) {
                return sendError(callback, "startDownload", AssetPackErrorCode.API_NOT_AVAILABLE)
            }
        }
        moduleNames.forEach {
            val moduleData = packageDownloadData[packageName]?.getModuleData(it)
            if (moduleData?.status != AssetPackStatus.DOWNLOADING && moduleData?.status != AssetPackStatus.COMPLETED) {
                packageDownloadData[packageName]?.updateDownloadStatus(it, AssetPackStatus.PENDING)
                sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, it, null, null)
            }
            if (moduleData?.status == AssetPackStatus.FAILED) {
                // FIXME: If we start download later, we shouldn't send a failure callback now
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NETWORK_ERROR) })
            }
            packageDownloadData[packageName]?.getModuleData(it)?.chunks?.filter { it.sliceId != null }?.forEach { chunkData ->
                val destination = chunkData.run {
                    File("${context.filesDir}/assetpacks/$sessionId/${this.moduleName}/$sliceId/", chunkIndex.toString())
                }

                if (destination.exists() && destination.length() == chunkData.chunkBytesToDownload) {
                    sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, it, chunkData, destination)
                }
            }
        }
        val bundleData = buildDownloadBundle(packageDownloadData[packageName]!!, list)
        Log.d(TAG, "startDownload: $bundleData")
        callback?.onStartDownload(-1, bundleData)
        moduleNames.forEach {
            val packData = packageDownloadData[packageName]?.getModuleData(it)
            if (packData?.status == AssetPackStatus.PENDING) {
                DownloadManager.get(context).shouldStop(false)
                DownloadManager.get(context).prepareDownload(packageDownloadData[packageName]!!, it)
            }
        }
    }

    override fun getSessionStates(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val options = bundle.getOptions()
        val installedAssetModules = bundle.getInstalledAssetModules()

        Log.d(TAG, "getSessionStates[$packageName](installedAssetModules: $installedAssetModules)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "getSessionStates", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || bundle == null) {
            return sendError(callback, "getSessionStates", AssetPackErrorCode.INVALID_REQUEST)
        }

        val listBundleData: MutableList<Bundle> = mutableListOf()

        packageName.takeIf { it == packageDownloadData[packageName]?.packageName }?.let {
            packageDownloadData[packageName]?.moduleNames?.forEach { moduleName ->
                if (moduleName in installedAssetModules) return@forEach

                listBundleData.add(sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName, null, null))

                packageDownloadData[packageName]?.getModuleData(moduleName)?.chunks?.filter { it.sliceId != null }?.forEach { chunkData ->
                    val destination = chunkData.run {
                        File("${context.filesDir}/assetpacks/$sessionId/${this.moduleName}/$sliceId/", chunkIndex.toString())
                    }

                    if (destination.exists() && destination.length() == chunkData.chunkBytesToDownload) {
                        sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName, chunkData, destination)
                    }
                }
            }
        }
        Log.d(TAG, "getSessionStates: $listBundleData")
        callback?.onGetSessionStates(listBundleData)
    }

    override fun notifyChunkTransferred(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val moduleName = bundle.get(BundleKeys.MODULE_NAME)
        val sliceId = bundle.get(BundleKeys.SLICE_ID)
        val chunkNumber = bundle.get(BundleKeys.CHUNK_NUMBER, 0)
        val sessionId = bundle.get(BundleKeys.SESSION_ID, packageDownloadData[packageName]?.sessionIds?.get(moduleName) ?: 0)
        val options = bundle2.getOptions()

        Log.d(TAG, "notifyChunkTransferred[$packageName](sessionId: $sessionId, moduleName: $moduleName, sliceId: $sliceId, chunkNumber: $chunkNumber)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "notifyChunkTransferred", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || bundle == null || moduleName.isNullOrEmpty() || sliceId.isNullOrEmpty()) {
            return sendError(callback, "notifyChunkTransferred", AssetPackErrorCode.INVALID_REQUEST)
        }

        if (packageDownloadData[packageName]?.sessionIds?.values?.contains(sessionId) != true) {
            Log.w(TAG, "No active session with id $sessionId in $packageName")
            return sendError(callback, "notifyChunkTransferred", AssetPackErrorCode.ACCESS_DENIED)
        }

        val downLoadFile = "${context.filesDir.absolutePath}/assetpacks/$sessionId/$moduleName/$sliceId/$chunkNumber"
        fileDescriptorMap[downLoadFile]?.close()
        fileDescriptorMap.remove(downLoadFile)
        // TODO: Remove chunk after successful transfer of chunk or only with module?
        callback?.onNotifyChunkTransferred(bundle, Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NO_ERROR) })
    }

    override fun notifyModuleCompleted(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val moduleName = bundle.get(BundleKeys.MODULE_NAME)
        val sessionId = bundle.get(BundleKeys.SESSION_ID, packageDownloadData[packageName]?.sessionIds?.get(moduleName) ?: 0)
        val options = bundle2.getOptions()

        Log.d(TAG, "notifyModuleCompleted[$packageName](sessionId: $sessionId, moduleName: $moduleName)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "notifyModuleCompleted", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || bundle == null || moduleName.isNullOrEmpty()) {
            return sendError(callback, "notifyModuleCompleted", AssetPackErrorCode.INVALID_REQUEST)
        }

        if (packageDownloadData[packageName]?.sessionIds?.values?.contains(sessionId) != true) {
            Log.w(TAG, "No active session with id $sessionId in $packageName")
            return sendError(callback, "notifyModuleCompleted", AssetPackErrorCode.ACCESS_DENIED)
        }

        packageDownloadData[packageName]?.updateDownloadStatus(moduleName, AssetPackStatus.COMPLETED)
        sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName, null, null)

        val downLoadFile = "${context.filesDir.absolutePath}/assetpacks/$sessionId/$moduleName"

        val directory = File(downLoadFile)
        if (directory.exists()) {
            directory.deleteRecursively()
            Log.d(TAG, "Directory $downLoadFile deleted successfully.")
        } else {
            Log.d(TAG, "Directory $downLoadFile does not exist.")
        }
        callback?.onNotifyModuleCompleted(bundle, bundle2)
    }

    override fun notifySessionFailed(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val sessionId = bundle.get(BundleKeys.SESSION_ID, 0)
        val options = bundle2.getOptions()

        Log.d(TAG, "notifySessionFailed[$packageName](sessionId: $sessionId)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "notifySessionFailed", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || bundle == null) {
            return sendError(callback, "notifySessionFailed", AssetPackErrorCode.INVALID_REQUEST)
        }

        if (packageDownloadData[packageName]?.sessionIds?.values?.contains(sessionId) != true) {
            Log.w(TAG, "No active session with id $sessionId in $packageName")
            return sendError(callback, "notifySessionFailed", AssetPackErrorCode.ACCESS_DENIED)
        }

        // TODO: Implement
        return sendError(callback, "notifySessionFailed", AssetPackErrorCode.API_NOT_AVAILABLE)
    }

    override fun keepAlive(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val options = bundle.getOptions()
        Log.d(TAG, "keepAlive[$packageName]()")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "keepAlive", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null) {
            return sendError(callback, "keepAlive", AssetPackErrorCode.INVALID_REQUEST)
        }

        // TODO: Implement
    }

    override fun getChunkFileDescriptor(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val moduleName = bundle.get(BundleKeys.MODULE_NAME)
        val sessionId = bundle.get(BundleKeys.SESSION_ID, packageDownloadData[packageName]?.sessionIds?.get(moduleName) ?: 0)
        val sliceId = bundle.get(BundleKeys.SLICE_ID)
        val chunkNumber = bundle.get(BundleKeys.CHUNK_NUMBER, 0)
        val options = bundle2.getOptions()

        Log.d(TAG, "getChunkFileDescriptor[$packageName](sessionId: $sessionId, moduleName: $moduleName, sliceId: $sliceId, chunkNumber: $chunkNumber)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "getChunkFileDescriptor", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || bundle == null || moduleName.isNullOrEmpty() || sliceId.isNullOrEmpty()) {
            return sendError(callback, "getChunkFileDescriptor", AssetPackErrorCode.INVALID_REQUEST)
        }

        if (packageDownloadData[packageName]?.sessionIds?.values?.contains(sessionId) != true) {
            Log.w(TAG, "No active session with id $sessionId in $packageName")
            return sendError(callback, "getChunkFileDescriptor", AssetPackErrorCode.ACCESS_DENIED)
        }

        val parcelFileDescriptor = runCatching {
            val downLoadFile = "${context.filesDir.absolutePath}/assetpacks/$sessionId/$moduleName/$sliceId/$chunkNumber"
            val filePath = Uri.parse(downLoadFile).path?.let { File(it) }
            ParcelFileDescriptor.open(filePath, ParcelFileDescriptor.MODE_READ_ONLY).also {
                fileDescriptorMap[downLoadFile] = it
            }
        }.getOrNull()
        callback?.onGetChunkFileDescriptor(Bundle().apply { put(BundleKeys.CHUNK_FILE_DESCRIPTOR, parcelFileDescriptor) }, Bundle())
    }

    override fun requestDownloadInfo(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val moduleNames = list?.getModuleNames()
        val options = bundle.getOptions()
        val installedAssetModules = bundle.getInstalledAssetModules()

        Log.d(TAG, "requestDownloadInfo[$packageName](moduleNames: $moduleNames, installedAssetModules: $installedAssetModules)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "requestDownloadInfo", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || moduleNames.isNullOrEmpty()) {
            return sendError(callback, "requestDownloadInfo", AssetPackErrorCode.INVALID_REQUEST)
        }

        if (packageDownloadData[packageName] == null ||
            packageDownloadData[packageName]?.packageName != packageName ||
            packageDownloadData[packageName]?.moduleNames?.intersect(moduleNames.toSet())?.isEmpty() == true) {
            packageDownloadData[packageName] = httpClient.initAssetModuleData(context, packageName, accountManager, moduleNames, options)
            if (packageDownloadData[packageName] == null) {
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
                return
            }
        }
        moduleNames.forEach {
            val packData = packageDownloadData[packageName]?.getModuleData(it)
            if (packData?.status == AssetPackStatus.FAILED) {
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NETWORK_ERROR) })
            }
        }
        val bundleData = buildDownloadBundle(packageDownloadData[packageName]!!, list)
        Log.d(TAG, "requestDownloadInfo: $bundleData")
        callback?.onRequestDownloadInfo(bundleData, bundleData)
    }

    override fun removeModule(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val moduleName = bundle?.get(BundleKeys.MODULE_NAME)
        val sessionId = bundle.get(BundleKeys.SESSION_ID, packageDownloadData[packageName]?.sessionIds?.get(moduleName) ?: 0)
        val options = bundle2.getOptions()

        Log.d(TAG, "removeModule[$packageName](sessionId: $sessionId, moduleName: $moduleName)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "removeModule", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || bundle == null || moduleName.isNullOrEmpty()) {
            return sendError(callback, "removeModule", AssetPackErrorCode.INVALID_REQUEST)
        }

        if (packageDownloadData[packageName]?.sessionIds?.values?.contains(sessionId) != true) {
            Log.w(TAG, "No active session with id $sessionId in $packageName")
            return sendError(callback, "removeModule", AssetPackErrorCode.ACCESS_DENIED)
        }

        // TODO: Implement
        return sendError(callback, "removeModule", AssetPackErrorCode.API_NOT_AVAILABLE)
    }

    override fun cancelDownloads(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)

        val moduleNames = list?.getModuleNames()
        val options = bundle.getOptions()

        Log.d(TAG, "cancelDownloads[$packageName](moduleNames: $moduleNames)")

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, "cancelDownloads", AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        if (packageName == null || moduleNames.isNullOrEmpty()) {
            return sendError(callback, "cancelDownloads", AssetPackErrorCode.INVALID_REQUEST)
        }

        // TODO: Implement
        return sendError(callback, "cancelDownloads", AssetPackErrorCode.API_NOT_AVAILABLE)
    }
}