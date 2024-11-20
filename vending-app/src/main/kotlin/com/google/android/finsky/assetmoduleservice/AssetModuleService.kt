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
import com.google.android.finsky.API_NOT_AVAILABLE
import com.google.android.finsky.DownloadManager
import com.google.android.finsky.KEY_BYTE_LENGTH
import com.google.android.finsky.KEY_CHUNK_FILE_DESCRIPTOR
import com.google.android.finsky.KEY_CHUNK_NAME
import com.google.android.finsky.KEY_CHUNK_NUMBER
import com.google.android.finsky.KEY_ERROR_CODE
import com.google.android.finsky.KEY_INDEX
import com.google.android.finsky.KEY_INSTALLED_ASSET_MODULE
import com.google.android.finsky.KEY_MODULE_NAME
import com.google.android.finsky.KEY_PLAY_CORE_VERSION_CODE
import com.google.android.finsky.KEY_RESOURCE_BLOCK_NAME
import com.google.android.finsky.KEY_RESOURCE_PACKAGE_NAME
import com.google.android.finsky.KEY_SESSION_ID
import com.google.android.finsky.KEY_SLICE_ID
import com.google.android.finsky.NETWORK_ERROR
import com.google.android.finsky.NO_ERROR
import com.google.android.finsky.STATUS_COMPLETED
import com.google.android.finsky.STATUS_DOWNLOADING
import com.google.android.finsky.STATUS_FAILED
import com.google.android.finsky.STATUS_INITIAL_STATE
import com.google.android.finsky.TAG_REQUEST
import com.google.android.finsky.buildDownloadBundle
import com.google.android.finsky.initAssertModuleData
import com.google.android.finsky.sendBroadcastForExistingFile
import com.google.android.play.core.assetpacks.protocol.IAssetModuleService
import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback
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
        return AssetModuleServiceImpl(this, lifecycle, httpClient, accountManager).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        httpClient.requestQueue.cancelAll(TAG_REQUEST)
        return super.onUnbind(intent)
    }
}

class AssetModuleServiceImpl(
    val context: Context, override val lifecycle: Lifecycle, private val httpClient: HttpClient, private val accountManager: AccountManager
) : IAssetModuleService.Stub(), LifecycleOwner {
    private val fileDescriptorMap = mutableMapOf<String, ParcelFileDescriptor>()

    override fun startDownload(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (startDownload) called by packageName -> $packageName")
        if (packageName == null || list == null || bundle == null || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
            return
        }
        if (downloadData == null || downloadData?.packageName != packageName) {
            val requestedAssetModuleNames = list.map { it.getString(KEY_MODULE_NAME) }.filter { !it.isNullOrEmpty() }
            val playCoreVersionCode = bundle.getInt(KEY_PLAY_CORE_VERSION_CODE)
            downloadData = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode)
            if (downloadData == null) {
                callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
                return
            }
        }
        list.forEach {
            val moduleName = it.getString(KEY_MODULE_NAME)
            val packData = downloadData?.getModuleData(moduleName!!)
            if (packData?.status != STATUS_DOWNLOADING && packData?.status != STATUS_COMPLETED) {
                downloadData?.updateDownloadStatus(moduleName!!, STATUS_INITIAL_STATE)
            }
            if (packData?.status == STATUS_FAILED) {
                callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, NETWORK_ERROR) })
            }
            downloadData?.getModuleData(moduleName!!)?.packBundleList?.forEach { dataBundle ->
                val destination = dataBundle.run {
                    val resourcePackageName = getString(KEY_RESOURCE_PACKAGE_NAME)
                    val chunkName = getString(KEY_CHUNK_NAME)
                    val resourceBlockName = getString(KEY_RESOURCE_BLOCK_NAME)
                    if (resourcePackageName == null || chunkName == null || resourceBlockName == null) return@forEach
                    File("${context.filesDir}/assetpacks/${getInt(KEY_INDEX)}/$resourcePackageName/$chunkName/", resourceBlockName)
                }

                if (destination.exists() && destination.length() == dataBundle.getLong(KEY_BYTE_LENGTH)) {
                    sendBroadcastForExistingFile(context, downloadData!!, moduleName, dataBundle, destination)
                }
            }
        }
        val bundleData = buildDownloadBundle(downloadData!!, list)
        Log.d(TAG, "startDownload: $bundleData")
        callback?.onStartDownload(-1, bundleData)
        list.forEach {
            val moduleName = it.getString(KEY_MODULE_NAME)
            val packData = downloadData?.getModuleData(moduleName!!)
            if (packData?.status == STATUS_INITIAL_STATE) {
                DownloadManager.get(context).shouldStop(false)
                DownloadManager.get(context).prepareDownload(downloadData!!, moduleName!!)
            }
        }
    }

    override fun getSessionStates(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (getSessionStates) called by packageName -> $packageName")
        val installedAssetModuleNames =
            bundle?.getParcelableArrayList<Bundle>(KEY_INSTALLED_ASSET_MODULE)?.flatMap { it.keySet().mapNotNull { subKey -> it.get(subKey) as? String } }
                ?.toMutableList() ?: mutableListOf()

        val listBundleData: MutableList<Bundle> = mutableListOf()

        packageName.takeIf { it == downloadData?.packageName }?.let {
            downloadData?.moduleNames?.forEach { moduleName ->
                if (moduleName in installedAssetModuleNames) return@forEach

                listBundleData.add(sendBroadcastForExistingFile(context, downloadData!!, moduleName, null, null))

                downloadData?.getModuleData(moduleName)?.packBundleList?.forEach { dataBundle ->
                    val destination = dataBundle.run {
                        val resourcePackageName = getString(KEY_RESOURCE_PACKAGE_NAME)
                        val chunkName = getString(KEY_CHUNK_NAME)
                        val resourceBlockName = getString(KEY_RESOURCE_BLOCK_NAME)
                        if (resourcePackageName == null || chunkName == null || resourceBlockName == null) return@forEach
                        File("${context.filesDir}/assetpacks/${getInt(KEY_INDEX)}/$resourcePackageName/$chunkName/", resourceBlockName)
                    }

                    if (destination.exists() && destination.length() == dataBundle.getLong(KEY_BYTE_LENGTH)) {
                        listBundleData.add(sendBroadcastForExistingFile(context, downloadData!!, moduleName, dataBundle, destination))
                    }
                }
            }
        }
        Log.d(TAG, "getSessionStates: $listBundleData")
        callback?.onGetSessionStates(listBundleData)
    }

    override fun notifyChunkTransferred(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifyChunkTransferred) called by packageName -> $packageName")
        val moduleName = bundle?.getString(KEY_MODULE_NAME)
        if (moduleName.isNullOrEmpty() || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
            return
        }
        val sessionId = bundle.getInt(KEY_SESSION_ID)
        val sliceId = bundle.getString(KEY_SLICE_ID)
        val chunkNumber = bundle.getInt(KEY_CHUNK_NUMBER)
        val downLoadFile = "${context.filesDir.absolutePath}/assetpacks/$sessionId/$moduleName/$sliceId/$chunkNumber"
        fileDescriptorMap[downLoadFile]?.close()
        fileDescriptorMap.remove(downLoadFile)
        callback?.onNotifyChunkTransferred(bundle, Bundle().apply { putInt(KEY_ERROR_CODE, NO_ERROR) })
    }

    override fun notifyModuleCompleted(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifyModuleCompleted) called but not implemented by packageName -> $packageName")
        val moduleName = bundle?.getString(KEY_MODULE_NAME)
        if (moduleName.isNullOrEmpty() || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
            return
        }
        Log.d(TAG, "notify: moduleName: $moduleName packNames: ${downloadData?.moduleNames}")
        downloadData?.updateDownloadStatus(moduleName, STATUS_COMPLETED)
        sendBroadcastForExistingFile(context, downloadData!!, moduleName, null, null)

        val sessionId = downloadData!!.sessionIds[moduleName]
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
        Log.d(TAG, "Method (notifySessionFailed) called but not implemented by packageName -> $packageName")
        callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
    }

    override fun keepAlive(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (keepAlive) called but not implemented by packageName -> $packageName")
    }

    override fun getChunkFileDescriptor(packageName: String, bundle: Bundle, bundle2: Bundle, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (getChunkFileDescriptor) called by packageName -> $packageName")
        val moduleName = bundle.getString(KEY_MODULE_NAME)
        if (moduleName.isNullOrEmpty() || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
            return
        }
        val parcelFileDescriptor = runCatching {
            val sessionId = bundle.getInt(KEY_SESSION_ID)
            val sliceId = bundle.getString(KEY_SLICE_ID)
            val chunkNumber = bundle.getInt(KEY_CHUNK_NUMBER)
            val downLoadFile = "${context.filesDir.absolutePath}/assetpacks/$sessionId/$moduleName/$sliceId/$chunkNumber"
            val filePath = Uri.parse(downLoadFile).path?.let { File(it) }
            ParcelFileDescriptor.open(filePath, ParcelFileDescriptor.MODE_READ_ONLY).also {
                fileDescriptorMap[downLoadFile] = it
            }
        }.getOrNull()
        callback?.onGetChunkFileDescriptor(Bundle().apply { putParcelable(KEY_CHUNK_FILE_DESCRIPTOR, parcelFileDescriptor) }, Bundle())
    }

    override fun requestDownloadInfo(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (requestDownloadInfo) called by packageName -> $packageName")
        if (packageName == null || list == null || bundle == null || !VendingPreferences.isAssetDeliveryEnabled(context)) {
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
            return
        }
        if (downloadData == null || downloadData?.packageName != packageName) {
            val requestedAssetModuleNames = list.map { it.getString(KEY_MODULE_NAME) }.filter { !it.isNullOrEmpty() }
            val playCoreVersionCode = bundle.getInt(KEY_PLAY_CORE_VERSION_CODE)
            downloadData = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode)
            if (downloadData == null) {
                callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
                return
            }
        }
        list.forEach {
            val moduleName = it.getString(KEY_MODULE_NAME)
            val packData = downloadData?.getModuleData(moduleName!!)
            if (packData?.status == STATUS_FAILED) {
                callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, NETWORK_ERROR) })
            }
        }
        val bundleData = buildDownloadBundle(downloadData!!, list)
        Log.d(TAG, "requestDownloadInfo: $bundleData")
        callback?.onRequestDownloadInfo(bundleData, bundleData)
    }

    override fun removeModule(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (removeModule) called but not implemented by packageName -> $packageName")
        callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
    }

    override fun cancelDownloads(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (cancelDownloads) called but not implemented by packageName -> $packageName")
        callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, API_NOT_AVAILABLE) })
    }

    companion object {
        @Volatile
        private var downloadData: DownloadData? = null
    }
}