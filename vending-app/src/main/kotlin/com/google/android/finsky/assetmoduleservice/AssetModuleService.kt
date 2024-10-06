/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.finsky.assetmoduleservice

import android.accounts.Account
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
import com.android.vending.licensing.AUTH_TOKEN_SCOPE
import com.android.vending.licensing.getAuthToken
import com.google.android.finsky.AssetModuleDeliveryRequest
import com.google.android.finsky.AssetModuleInfo
import com.google.android.finsky.CallerInfo
import com.google.android.finsky.CallerState
import com.google.android.finsky.KEY_CHUNK_FILE_DESCRIPTOR
import com.google.android.finsky.KEY_CHUNK_NUMBER
import com.google.android.finsky.KEY_ERROR_CODE
import com.google.android.finsky.KEY_MODULE_NAME
import com.google.android.finsky.KEY_PACK_NAMES
import com.google.android.finsky.KEY_PLAY_CORE_VERSION_CODE
import com.google.android.finsky.KEY_RESOURCE_PACKAGE_NAME
import com.google.android.finsky.KEY_SESSION_ID
import com.google.android.finsky.KEY_SLICE_ID
import com.google.android.finsky.PageSource
import com.google.android.finsky.STATUS_COMPLETED
import com.google.android.finsky.STATUS_DOWNLOADING
import com.google.android.finsky.STATUS_NOT_INSTALLED
import com.google.android.finsky.STATUS_TRANSFERRING
import com.google.android.finsky.TAG_REQUEST
import com.google.android.finsky.buildDownloadBundle
import com.google.android.finsky.downloadFile
import com.google.android.finsky.getAppVersionCode
import com.google.android.finsky.initModuleDownloadInfo
import com.google.android.finsky.requestAssetModule
import com.google.android.finsky.sendBroadcastForExistingFile
import com.google.android.play.core.assetpacks.protocol.IAssetModuleService
import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import okhttp3.internal.filterList
import org.microg.gms.auth.AuthConstants
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
    private val sharedModuleDataFlow = MutableSharedFlow<ModuleData>()
    private var moduleData: ModuleData? = null

    override fun startDownload(packageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (startDownload) called by packageName -> $packageName")
        if (packageName == null || list == null || bundle == null) {
            Log.d(TAG, "startDownload: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
            return
        }
        if (moduleData == null && moduleErrorRequested.contains(packageName)) {
            Log.d(TAG, "startDownload: moduleData request error")
            val result = Bundle().apply { putStringArrayList(KEY_PACK_NAMES, arrayListOf<String>()) }
            callback?.onStartDownload(-1, result)
            return
        }
        suspend fun prepare(data: ModuleData) {
            list.forEach {
                val moduleName = it.getString(KEY_MODULE_NAME)
                if (moduleName != null) {
                    callback?.onStartDownload(-1, buildDownloadBundle(moduleName, data, true))
                    val packData = data.getPackData(moduleName)
                    if (packData?.status != STATUS_NOT_INSTALLED) {
                        Log.w(TAG, "startDownload: packData?.status is ${packData?.status}")
                        return@forEach
                    }
                    data.updateDownloadStatus(moduleName, STATUS_DOWNLOADING)
                    data.updateModuleDownloadStatus(STATUS_DOWNLOADING)
                    packData.bundleList?.forEach { download ->
                        if (moduleName == download.getString(KEY_RESOURCE_PACKAGE_NAME)) {
                            httpClient.downloadFile(context, moduleName, data, download)
                        }
                    }
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            if (moduleData == null) {
                sharedModuleDataFlow.collectLatest { prepare(it) }
                return@launchWhenStarted
            }
            prepare(moduleData!!)
        }
    }

    override fun getSessionStates(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (getSessionStates) called by packageName -> $packageName")
    }

    override fun notifyChunkTransferred(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifyChunkTransferred) called but not implemented by packageName -> $packageName")
        callback?.onNotifyChunkTransferred(bundle, Bundle())
    }

    override fun notifyModuleCompleted(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifyModuleCompleted) called but not implemented by packageName -> $packageName")
        val moduleName = bundle?.getString(KEY_MODULE_NAME)
        if (moduleName.isNullOrEmpty()) {
            Log.d(TAG, "notifyModuleCompleted: params invalid ")
            callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
            return
        }
        fun notify(data: ModuleData) {
            callback?.onNotifyModuleCompleted(bundle, bundle)
            val notify = data.packNames?.all { data.getPackData(it)?.status == STATUS_TRANSFERRING } ?: false
            if (notify) {
                data.packNames?.forEach { moduleData?.updateDownloadStatus(it, STATUS_COMPLETED) }
                data.updateModuleDownloadStatus(STATUS_COMPLETED)
                sendBroadcastForExistingFile(context, moduleData!!, moduleName, null, null)
            }
        }
        lifecycleScope.launchWhenStarted {
            if (moduleData == null) {
                sharedModuleDataFlow.collectLatest { notify(it) }
                return@launchWhenStarted
            }
            notify(moduleData!!)
        }
    }

    override fun notifySessionFailed(packageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (notifySessionFailed) called but not implemented by packageName -> $packageName")
    }

    override fun keepAlive(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (keepAlive) called but not implemented by packageName -> $packageName")
    }

    override fun getChunkFileDescriptor(packageName: String, bundle: Bundle, bundle2: Bundle, callback: IAssetModuleServiceCallback?) {
        Log.d(TAG, "Method (getChunkFileDescriptor) called but not implemented by packageName -> $packageName")
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
            ParcelFileDescriptor.open(filePath, ParcelFileDescriptor.MODE_READ_ONLY)
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
            val requestedAssetModuleNames = arrayListOf<String>()
            for (data in list) {
                val moduleName = data.getString(KEY_MODULE_NAME)
                if (!moduleName.isNullOrEmpty()) {
                    requestedAssetModuleNames.add(moduleName)
                }
            }
            if (!moduleData?.packNames.isNullOrEmpty()) {
                val bundleData = Bundle().apply {
                    val isPack = moduleData?.packNames?.size != requestedAssetModuleNames.size
                    requestedAssetModuleNames.forEach {
                        putAll(buildDownloadBundle(it, moduleData!!, isPack, packNames = requestedAssetModuleNames))
                    }
                }
                callback?.onRequestDownloadInfo(bundleData, bundleData)
                return@launchWhenStarted
            }
            val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
            var oauthToken: String? = null
            if (accounts.isEmpty()) {
                callback?.onError(Bundle().apply { putInt(KEY_ERROR_CODE, -5) })
                return@launchWhenStarted
            } else for (account: Account in accounts) {
                oauthToken = accountManager.getAuthToken(account, AUTH_TOKEN_SCOPE, false).getString(AccountManager.KEY_AUTHTOKEN)
                if (oauthToken != null) {
                    break
                }
            }
            val requestPayload =
                AssetModuleDeliveryRequest.Builder().callerInfo(CallerInfo(getAppVersionCode(context, packageName)?.toInt())).packageName(packageName)
                    .playCoreVersion(bundle.getInt(KEY_PLAY_CORE_VERSION_CODE))
                    .pageSource(listOf(PageSource.UNKNOWN_SEARCH_TRAFFIC_SOURCE, PageSource.BOOKS_HOME_PAGE))
                    .callerState(listOf(CallerState.CALLER_APP_REQUEST, CallerState.CALLER_APP_DEBUGGABLE)).moduleInfo(ArrayList<AssetModuleInfo>().apply {
                        list.filterList { !getString(KEY_MODULE_NAME).isNullOrEmpty() }.forEach {
                            add(AssetModuleInfo.Builder().name(it.getString(KEY_MODULE_NAME)).build())
                        }
                    }).build()
            val moduleDeliveryInfo = httpClient.requestAssetModule(context, oauthToken!!, requestPayload)
            if (moduleDeliveryInfo == null || moduleDeliveryInfo.status != null) {
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
            Log.d(TAG, "requestDownloadInfo: moduleDeliveryInfo-> $moduleDeliveryInfo")
            moduleData = initModuleDownloadInfo(context, packageName, moduleDeliveryInfo)
            val bundleData = Bundle().apply {
                val isPack = moduleData?.packNames?.size != requestedAssetModuleNames.size
                requestedAssetModuleNames.forEach {
                    putAll(buildDownloadBundle(it, moduleData!!, isPack, packNames = requestedAssetModuleNames))
                }
            }
            callback?.onRequestDownloadInfo(bundleData, bundleData)
            sharedModuleDataFlow.emit(moduleData!!)
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