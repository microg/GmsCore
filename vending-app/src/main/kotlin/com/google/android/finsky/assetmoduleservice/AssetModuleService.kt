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
import com.google.android.finsky.DownloadManager
import com.google.android.finsky.ERROR_CODE_FAIL
import com.google.android.finsky.KEY_CHUNK_FILE_DESCRIPTOR
import com.google.android.finsky.KEY_CHUNK_NUMBER
import com.google.android.finsky.KEY_ERROR_CODE
import com.google.android.finsky.KEY_MODULE_NAME
import com.google.android.finsky.KEY_PACK_NAMES
import com.google.android.finsky.KEY_PLAY_CORE_VERSION_CODE
import com.google.android.finsky.KEY_SESSION_ID
import com.google.android.finsky.KEY_SLICE_ID
import com.google.android.finsky.STATUS_COMPLETED
import com.google.android.finsky.STATUS_DOWNLOADING
import com.google.android.finsky.STATUS_INITIAL_STATE
import com.google.android.finsky.STATUS_NOT_INSTALLED
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
        if (packageName == null || list == null || bundle == null) {
            Log.d(TAG, "startDownload: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
            return
        }
        lifecycleScope.launchWhenStarted {
            val requestedAssetModuleNames = list.map { it.getString(KEY_MODULE_NAME) }.filter { !it.isNullOrEmpty() }
            if (requestedAssetModuleNames.isEmpty() || moduleErrorRequested[packageName]?.contains(requestedAssetModuleNames.joinToString()) == true) {
                Log.d(TAG, "startDownload: moduleData request error")
                val result = Bundle().apply { putStringArrayList(KEY_PACK_NAMES, arrayListOf<String>()) }
                callback?.onStartDownload(-1, result)
                return@launchWhenStarted
            }
            if (downloadData == null || downloadData?.packageName != packageName) {
                val playCoreVersionCode = bundle.getInt(KEY_PLAY_CORE_VERSION_CODE)
                downloadData = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode)
            }
            if (downloadData?.status == STATUS_NOT_INSTALLED) {
                downloadData?.moduleNames?.forEach{
                    downloadData?.updateDownloadStatus(it, STATUS_INITIAL_STATE)
                }
                downloadData?.updateModuleDownloadStatus(STATUS_INITIAL_STATE)
                val bundleData = buildDownloadBundle(downloadData!!,list)
                DownloadManager.get(context).prepareDownload(downloadData!!)
                Log.d(TAG, "startDownload---1${bundleData}")
                callback?.onStartDownload(-1, bundleData)
                downloadData?.moduleNames?.forEach{
                    downloadData?.updateDownloadStatus(it, STATUS_DOWNLOADING)
                }
                downloadData?.updateModuleDownloadStatus(STATUS_DOWNLOADING)
                return@launchWhenStarted
            }
            val bundleData = buildDownloadBundle(downloadData!!,list)
            Log.d(TAG, "startDownload---2${bundleData}")
            callback?.onStartDownload(-1, bundleData)
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
            Log.d(TAG, "notify: moduleName: $moduleName packNames: ${downloadData?.moduleNames}")
            downloadData?.updateDownloadStatus(moduleName, STATUS_COMPLETED)
            sendBroadcastForExistingFile(context, downloadData!!, moduleName, null, null)
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
            if (downloadData == null || downloadData?.packageName != packageName) {
                val playCoreVersionCode = bundle.getInt(KEY_PLAY_CORE_VERSION_CODE)
                downloadData = httpClient.initAssertModuleData(context, packageName, accountManager, requestedAssetModuleNames, playCoreVersionCode)
            }
            Log.d(TAG, "requestDownloadInfo: $requestedAssetModuleNames ")
            if (downloadData?.errorCode == ERROR_CODE_FAIL) {
                val errorModule = moduleErrorRequested[packageName]
                if (!errorModule.isNullOrEmpty() && !errorModule.contains(requestedAssetModuleNames.joinToString())) {
                    callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
                    return@launchWhenStarted
                }
                Log.d(TAG, "requestDownloadInfo: error by $packageName ")
                moduleErrorRequested[packageName] = moduleErrorRequested[packageName]?.apply { add(requestedAssetModuleNames.joinToString()) }
                    ?: arrayListOf(requestedAssetModuleNames.joinToString())
                val result = Bundle().apply { putStringArrayList(KEY_PACK_NAMES, ArrayList(requestedAssetModuleNames)) }
                callback?.onRequestDownloadInfo(result, result)
                return@launchWhenStarted
            }
            moduleErrorRequested.remove(packageName)
            val bundleData = buildDownloadBundle(downloadData!!,list)
            Log.d(TAG, "requestDownloadInfo---${bundleData}")
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
        @Volatile
        private var downloadData: DownloadData? = null
        private val moduleErrorRequested = HashMap<String, ArrayList<String>>()
    }
}