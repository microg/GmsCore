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
import androidx.lifecycle.lifecycleScope
import com.google.android.finsky.ERROR_CODE_FAIL
import com.google.android.finsky.KEY_APP_VERSION_CODE
import com.google.android.finsky.KEY_BYTES_DOWNLOADED
import com.google.android.finsky.KEY_CHUNK_FILE_DESCRIPTOR
import com.google.android.finsky.KEY_CHUNK_NUMBER
import com.google.android.finsky.KEY_ERROR_CODE
import com.google.android.finsky.KEY_MODULE_NAME
import com.google.android.finsky.KEY_PACK_BASE_VERSION
import com.google.android.finsky.KEY_PACK_NAMES
import com.google.android.finsky.KEY_PACK_VERSION
import com.google.android.finsky.KEY_PLAY_CORE_VERSION_CODE
import com.google.android.finsky.KEY_RESOURCE_PACKAGE_NAME
import com.google.android.finsky.KEY_SESSION_ID
import com.google.android.finsky.KEY_SLICE_ID
import com.google.android.finsky.KEY_STATUS
import com.google.android.finsky.KEY_TOTAL_BYTES_TO_DOWNLOAD
import com.google.android.finsky.STATUS_COMPLETED
import com.google.android.finsky.STATUS_DOWNLOADING
import com.google.android.finsky.STATUS_INITIAL_STATE
import com.google.android.finsky.TAG_REQUEST
import com.google.android.finsky.buildDownloadBundle
import com.google.android.finsky.combineModule
import com.google.android.finsky.downloadFile
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
    @Volatile
    private var moduleData: ModuleData? = null
    private val fileDescriptorMap = mutableMapOf<String, ParcelFileDescriptor>()

    override fun startDownload(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (startDownload) called by packageName -> $packageName")
        if (packageName == null || list == null || bundle == null) {
            Log.d(TAG, "startDownload: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
            return
        }
        lifecycleScope.launchWhenStarted {
            if (list.all { it.getString(KEY_MODULE_NAME) == null }) {
                Log.d(TAG, "startDownload: module name is null")
                val result = Bundle().apply { putStringArrayList(KEY_PACK_NAMES, arrayListOf<String>()) }
                callback?.onStartDownload(-1, result)
                return@launchWhenStarted
            }
            if (moduleErrorRequested.contains(packageName)) {
                Log.d(TAG, "startDownload: moduleData request error")
                val result = Bundle().apply { putStringArrayList(KEY_PACK_NAMES, arrayListOf<String>()) }
                Log.d(TAG, "prepare: ${result.keySet()}")
                callback?.onStartDownload(-1, result)
                return@launchWhenStarted
            }
            if (moduleData?.status != STATUS_DOWNLOADING) {
                val requestedAssetModuleNames = list.map { it.getString(KEY_MODULE_NAME) }.filter { !it.isNullOrEmpty() }
                if (moduleData == null) {
                    val playCoreVersionCode = bundle.getInt(KEY_PLAY_CORE_VERSION_CODE)
                    moduleData = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode)
                }
                list.forEach {
                    val moduleName = it.getString(KEY_MODULE_NAME)
                    if (moduleName != null) {
                        val packData = moduleData!!.getPackData(moduleName)
                        callback?.onStartDownload(-1, buildDownloadBundle(moduleName, moduleData!!, true))
                        if (packData?.status != STATUS_INITIAL_STATE) {
                            Log.w(TAG, "startDownload: packData?.status is ${packData?.status}")
                            return@forEach
                        }
                        moduleData?.updateDownloadStatus(moduleName, STATUS_DOWNLOADING)
                        moduleData?.updateModuleDownloadStatus(STATUS_DOWNLOADING)
                        packData.bundleList?.forEach { download ->
                            if (moduleName == download.getString(KEY_RESOURCE_PACKAGE_NAME)) {
                                downloadFile(context, moduleName, moduleData!!, download)
                            }
                        }
                    }
                }
                return@launchWhenStarted
            }
            val result = Bundle()
            val arrayList = arrayListOf<String>()
            result.putInt(KEY_STATUS, moduleData!!.status)
            result.putLong(KEY_APP_VERSION_CODE, moduleData!!.appVersionCode)
            result.putLong(KEY_BYTES_DOWNLOADED, moduleData!!.bytesDownloaded)
            result.putInt(KEY_ERROR_CODE, moduleData!!.errorCode)
            result.putInt(KEY_SESSION_ID, 6)
            result.putLong(KEY_TOTAL_BYTES_TO_DOWNLOAD, moduleData!!.totalBytesToDownload)
            list.forEach {
                val moduleName = it.getString(KEY_MODULE_NAME)
                arrayList.add(moduleName!!)
                val packData = moduleData!!.getPackData(moduleName)
                result.putInt(combineModule(KEY_SESSION_ID, moduleName), packData!!.sessionId)
                result.putInt(combineModule(KEY_STATUS, moduleName), packData.status)
                result.putInt(combineModule(KEY_ERROR_CODE, moduleName), packData.errorCode)
                result.putLong(combineModule(KEY_PACK_VERSION, moduleName), packData.packVersion)
                result.putLong(combineModule(KEY_PACK_BASE_VERSION, moduleName), packData.packBaseVersion)
                result.putLong(combineModule(KEY_BYTES_DOWNLOADED, moduleName), packData.bytesDownloaded)
                result.putLong(combineModule(KEY_TOTAL_BYTES_TO_DOWNLOAD, moduleName), packData.totalBytesToDownload)
                sendBroadcastForExistingFile(context, moduleData!!, moduleName, null, null)
            }
            result.putStringArrayList(KEY_PACK_NAMES, arrayList)
            callback?.onStartDownload(-1, result)
        }
    }

    override fun getSessionStates(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (getSessionStates) called by packageName -> $packageName")
    }

    override fun notifyChunkTransferred(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifyChunkTransferred) called by packageName -> $packageName")
        val moduleName = bundle?.getString(KEY_MODULE_NAME)
        if (moduleName.isNullOrEmpty()) {
            Log.d(TAG, "notifyChunkTransferred: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
            return
        }
        val sessionId = bundle.getInt(KEY_SESSION_ID)
        val sliceId = bundle.getString(KEY_SLICE_ID)
        val chunkNumber = bundle.getInt(KEY_CHUNK_NUMBER)
        val downLoadFile = "${context.filesDir.absolutePath}/assetpacks/$sessionId/$moduleName/$sliceId/$chunkNumber"
        fileDescriptorMap[downLoadFile]?.close()
        fileDescriptorMap.remove(downLoadFile)
        callback?.onNotifyChunkTransferred(bundle, Bundle().apply { putInt(KEY_ERROR_CODE, 0) })
    }

    override fun notifyModuleCompleted(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifyModuleCompleted) called but not implemented by packageName -> $packageName")
        val moduleName = bundle?.getString(KEY_MODULE_NAME)
        if (moduleName.isNullOrEmpty()) {
            Log.d(TAG, "notifyModuleCompleted: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
            return
        }
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "notify: moduleName: $moduleName packNames: ${moduleData?.packNames}")
            moduleData?.packNames?.find { it == moduleName }?.let {
                moduleData?.updateDownloadStatus(it, STATUS_COMPLETED)
                if (moduleData?.packNames?.all { pack -> moduleData?.getPackData(pack)?.status == STATUS_COMPLETED } == true) {
                    moduleData?.updateModuleDownloadStatus(STATUS_COMPLETED)
                }
                sendBroadcastForExistingFile(context, moduleData!!, moduleName, null, null)
            }
            callback?.onNotifyModuleCompleted(bundle, bundle)
        }
    }

    override fun notifySessionFailed(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifySessionFailed) called but not implemented by packageName -> $packageName")
    }

    override fun keepAlive(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (keepAlive) called but not implemented by packageName -> $packageName")
    }

    override fun getChunkFileDescriptor(packageName: String, bundle: Bundle, bundle2: Bundle, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (getChunkFileDescriptor) called by packageName -> $packageName")
        val moduleName = bundle.getString(KEY_MODULE_NAME)
        if (moduleName.isNullOrEmpty()) {
            Log.d(TAG, "getChunkFileDescriptor: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
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
        if (packageName == null || list == null || bundle == null) {
            Log.w(TAG, "requestDownloadInfo: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
            return
        }
        lifecycleScope.launchWhenStarted {
            val requestedAssetModuleNames = list.map { it.getString(KEY_MODULE_NAME) }.filter { !it.isNullOrEmpty() }
            if (moduleData == null) {
                val playCoreVersionCode = bundle.getInt(KEY_PLAY_CORE_VERSION_CODE)
                moduleData = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode)
            }
            if (moduleData?.errorCode == ERROR_CODE_FAIL) {
                if (moduleErrorRequested.contains(packageName)) {
                    callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
                    return@launchWhenStarted
                }
                moduleErrorRequested.add(packageName)
                val result = Bundle().apply { putStringArrayList(KEY_PACK_NAMES, arrayListOf<String>()) }
                callback?.onRequestDownloadInfo(result, result)
                return@launchWhenStarted
            }
            moduleErrorRequested.remove(packageName)
            val bundleData = Bundle().apply {
                val isPack = moduleData?.packNames?.size != requestedAssetModuleNames.size
                requestedAssetModuleNames.forEach {
                    putAll(buildDownloadBundle(it!!, moduleData!!, isPack, packNames = requestedAssetModuleNames))
                }
            }
            callback?.onRequestDownloadInfo(bundleData, bundleData)
        }
    }

    override fun removeModule(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (removeModule) called but not implemented by packageName -> $packageName")
    }

    override fun cancelDownloads(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (cancelDownloads) called but not implemented by packageName -> $packageName")
    }

    companion object {
        private val moduleErrorRequested = arrayListOf<String>()
    }
}