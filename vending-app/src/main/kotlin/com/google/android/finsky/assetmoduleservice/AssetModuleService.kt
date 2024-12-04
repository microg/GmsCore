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

    override fun startDownload(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)
        Log.d(TAG, "Method (startDownload) called by packageName -> $packageName")
        if (packageName == null || list == null || bundle == null || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
            return
        }
        val requestedAssetModuleNames = list.map { it.getString(KEY_MODULE_NAME) }.filter { !it.isNullOrEmpty() }
        if (packageDownloadData[packageName] == null ||
            packageDownloadData[packageName]?.packageName != packageName ||
            packageDownloadData[packageName]?.moduleNames?.intersect(requestedAssetModuleNames.toSet())?.isEmpty() == true
        ) {
            val playCoreVersionCode = bundle.get(BundleKeys.PLAY_CORE_VERSION_CODE)
            packageDownloadData[packageName] = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode ?: 0)
            if (packageDownloadData[packageName] == null) {
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
                return
            }
        }
        list.forEach { data ->
            val moduleName = data.get(BundleKeys.MODULE_NAME)
            val moduleData = packageDownloadData[packageName]?.getModuleData(moduleName!!)
            if (moduleData?.status != AssetPackStatus.DOWNLOADING && moduleData?.status != AssetPackStatus.COMPLETED) {
                packageDownloadData[packageName]?.updateDownloadStatus(moduleName!!, AssetPackStatus.PENDING)
                sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName!!, null, null)
            }
            if (moduleData?.status == AssetPackStatus.FAILED) {
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NETWORK_ERROR) })
            }
            packageDownloadData[packageName]?.getModuleData(moduleName!!)?.chunks?.filter { it.sliceId != null }?.forEach { chunkData ->
                val destination = chunkData.run {
                    File("${context.filesDir}/assetpacks/$sessionId/${this.moduleName}/$sliceId/", chunkIndex.toString())
                }

                if (destination.exists() && destination.length() == chunkData.chunkBytesToDownload) {
                    sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName, chunkData, destination)
                }
            }
        }
        val bundleData = buildDownloadBundle(packageDownloadData[packageName]!!, list)
        Log.d(TAG, "startDownload: $bundleData")
        callback?.onStartDownload(-1, bundleData)
        list.forEach {
            val moduleName = it.get(BundleKeys.MODULE_NAME)
            val packData = packageDownloadData[packageName]?.getModuleData(moduleName!!)
            if (packData?.status == AssetPackStatus.PENDING) {
                DownloadManager.get(context).shouldStop(false)
                DownloadManager.get(context).prepareDownload(packageDownloadData[packageName]!!, moduleName!!)
            }
        }
    }

    override fun getSessionStates(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)
        Log.d(TAG, "Method (getSessionStates) called by packageName -> $packageName")
        val installedAssetModuleNames =
            bundle?.get(BundleKeys.INSTALLED_ASSET_MODULE)?.flatMap { it.keySet().mapNotNull { subKey -> it.get(subKey) as? String } }
                ?.toMutableList() ?: mutableListOf()

        val listBundleData: MutableList<Bundle> = mutableListOf()

        packageName.takeIf { it == packageDownloadData[packageName]?.packageName }?.let {
            packageDownloadData[packageName]?.moduleNames?.forEach { moduleName ->
                if (moduleName in installedAssetModuleNames) return@forEach

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
        Log.d(TAG, "Method (notifyChunkTransferred) called by packageName -> $packageName")
        val moduleName = bundle?.get(BundleKeys.MODULE_NAME)
        if (moduleName.isNullOrEmpty() || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
            return
        }
        val sessionId = bundle.get(BundleKeys.SESSION_ID)
        val sliceId = bundle.get(BundleKeys.SLICE_ID)
        val chunkNumber = bundle.get(BundleKeys.CHUNK_NUMBER)
        val downLoadFile = "${context.filesDir.absolutePath}/assetpacks/$sessionId/$moduleName/$sliceId/$chunkNumber"
        fileDescriptorMap[downLoadFile]?.close()
        fileDescriptorMap.remove(downLoadFile)
        callback?.onNotifyChunkTransferred(bundle, Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NO_ERROR) })
    }

    override fun notifyModuleCompleted(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)
        Log.d(TAG, "Method (notifyModuleCompleted) called but not implemented by packageName -> $packageName")
        val moduleName = bundle?.get(BundleKeys.MODULE_NAME)
        if (moduleName.isNullOrEmpty() || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
            return
        }
        Log.d(TAG, "notify: moduleName: $moduleName packNames: ${packageDownloadData[packageName]?.moduleNames}")
        packageDownloadData[packageName]?.updateDownloadStatus(moduleName, AssetPackStatus.COMPLETED)
        sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName, null, null)

        val sessionId = packageDownloadData[packageName]!!.sessionIds[moduleName]
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
        Log.d(TAG, "Method (notifySessionFailed) called but not implemented by packageName -> $packageName")
        callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
    }

    override fun keepAlive(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)
        Log.d(TAG, "Method (keepAlive) called but not implemented by packageName -> $packageName")
    }

    override fun getChunkFileDescriptor(packageName: String, bundle: Bundle, bundle2: Bundle, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)
        Log.d(TAG, "Method (getChunkFileDescriptor) called by packageName -> $packageName")
        val moduleName = bundle.get(BundleKeys.MODULE_NAME)
        if (moduleName.isNullOrEmpty() || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
            return
        }
        val parcelFileDescriptor = runCatching {
            val sessionId = bundle.get(BundleKeys.SESSION_ID)
            val sliceId = bundle.get(BundleKeys.SLICE_ID)
            val chunkNumber = bundle.get(BundleKeys.CHUNK_NUMBER)
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
        Log.d(TAG, "Method (requestDownloadInfo) called by packageName -> $packageName")
        if (packageName == null || list == null || bundle == null || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
            return
        }
        val requestedAssetModuleNames = list.map { it.getString(KEY_MODULE_NAME) }.filter { !it.isNullOrEmpty() }
        if (packageDownloadData[packageName] == null ||
            packageDownloadData[packageName]?.packageName != packageName ||
            packageDownloadData[packageName]?.moduleNames?.intersect(requestedAssetModuleNames.toSet())?.isEmpty() == true
            ) {
            val playCoreVersionCode = bundle.get(BundleKeys.PLAY_CORE_VERSION_CODE) ?: 0
            packageDownloadData[packageName] = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode)
            if (packageDownloadData[packageName] == null) {
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
                return
            }
        }
        list.forEach {
            val moduleName = it.get(BundleKeys.MODULE_NAME)
            val packData = packageDownloadData[packageName]?.getModuleData(moduleName!!)
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
        Log.d(TAG, "Method (removeModule) called but not implemented by packageName -> $packageName")
        callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
    }

    override fun cancelDownloads(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        PackageUtils.getAndCheckCallingPackage(context, packageName)
        Log.d(TAG, "Method (cancelDownloads) called but not implemented by packageName -> $packageName")
        callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.API_NOT_AVAILABLE) })
    }
}