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
import androidx.lifecycle.LifecycleService
import com.google.android.finsky.*
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.assetpacks.protocol.BundleKeys
import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.core.HttpClient
import java.io.File

const val TAG = "AssetModuleService"

class AssetModuleService : LifecycleService() {
    private lateinit var httpClient: HttpClient
    private lateinit var accountManager: AccountManager

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind: ")
        ProfileManager.ensureInitialized(this)
        accountManager = AccountManager.get(this)
        httpClient = HttpClient()
        return AssetModuleServiceImpl(this, lifecycle, httpClient, accountManager, packageDownloadData).asBinder()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        // TODO cancel downloads
        super.onDestroy()
    }

    companion object {
        private val packageDownloadData = mutableMapOf<String, DownloadData?>()
    }
}

class AssetModuleServiceImpl(
    context: Context, lifecycle: Lifecycle,
    private val httpClient: HttpClient,
    private val accountManager: AccountManager,
    private val packageDownloadData: MutableMap<String, DownloadData?>
) : AbstractAssetModuleServiceImpl(context, lifecycle) {
    private val fileDescriptorMap = mutableMapOf<File, ParcelFileDescriptor>()
    private val lock = Any()

    private fun checkSessionValid(packageName: String, sessionId: Int) {
        Log.d(TAG, "checkSessionValid: $packageName $sessionId ${packageDownloadData[packageName]?.sessionIds}")
        if (packageDownloadData[packageName]?.sessionIds?.values?.contains(sessionId) != true) {
            Log.w(TAG, "No active session with id $sessionId in $packageName")
            throw AssetPackException(AssetPackErrorCode.ACCESS_DENIED)
        }
    }

    override fun getDefaultSessionId(packageName: String, moduleName: String): Int = synchronized(lock) {
        packageDownloadData[packageName]?.sessionIds?.get(moduleName) ?: 0
    }

    override suspend fun startDownload(params: StartDownloadParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        val needInit = synchronized(lock) {
            packageDownloadData[packageName] == null ||
                packageDownloadData[packageName]?.packageName != packageName ||
                packageDownloadData[packageName]?.moduleNames?.intersect(params.moduleNames.toSet())?.isEmpty() == true
        }

        if (needInit) {
            val newData = httpClient.initAssetModuleData(context, packageName, accountManager, params.moduleNames, params.options)
            synchronized(lock) {
                packageDownloadData[packageName] = packageDownloadData[packageName].merge(newData)
            }
            if (packageDownloadData[packageName] == null) {
                throw AssetPackException(AssetPackErrorCode.API_NOT_AVAILABLE)
            }
        }
        if (packageDownloadData[packageName] != null && packageDownloadData[packageName]?.moduleNames?.all {
                packageDownloadData[packageName]?.getModuleData(it)?.status == AssetPackStatus.COMPLETED
            } == true && params.installedAssetModules.isEmpty()) {
            Log.d(TAG, "startDownload: resetAllModuleStatus ")
            packageDownloadData[packageName]?.resetAllModuleStatus()
        }
        params.moduleNames.forEach {
            val moduleData = packageDownloadData[packageName]?.getModuleData(it)
            if (moduleData?.status != AssetPackStatus.DOWNLOADING && moduleData?.status != AssetPackStatus.COMPLETED) {
                packageDownloadData[packageName]?.updateDownloadStatus(it, AssetPackStatus.PENDING)
                sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, it, null, null)
            }
            if (moduleData?.status == AssetPackStatus.FAILED) {
                // FIXME: If we start download later, we shouldn't send a failure callback now
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NETWORK_ERROR) })
            }
            packageDownloadData[packageName]?.getModuleData(it)?.chunks?.forEach { chunkData ->
                val destination = chunkData.getChunkFile(context)
                if (destination.exists() && destination.length() == chunkData.chunkBytesToDownload) {
                    sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, it, chunkData, destination)
                }
            }
        }
        val bundleData = buildDownloadBundle(packageDownloadData[packageName]!!, params.moduleNames)
        Log.d(TAG, "startDownload: $bundleData")
        callback?.onStartDownload(-1, bundleData)
        params.moduleNames.forEach {
            val packData = packageDownloadData[packageName]?.getModuleData(it)
            if (packData?.status == AssetPackStatus.PENDING) {
                DownloadManager.get(context).shouldStop(false)
                DownloadManager.get(context).prepareDownload(packageDownloadData[packageName]!!, it)
            }
        }
    }

    override suspend fun getSessionStates(params: GetSessionStatesParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        val listBundleData: MutableList<Bundle> = mutableListOf()

        synchronized(lock) {
            if (packageDownloadData[packageName] != null && packageDownloadData[packageName]?.moduleNames?.all {
                    packageDownloadData[packageName]?.getModuleData(it)?.status == AssetPackStatus.COMPLETED
                } == true && params.installedAssetModules.isEmpty()) {
                Log.d(TAG, "getSessionStates: resetAllModuleStatus: $listBundleData")
                packageDownloadData[packageName]?.resetAllModuleStatus()
                callback?.onGetSessionStates(listBundleData)
                return
            }

            packageDownloadData[packageName]?.moduleNames?.forEach { moduleName ->
                if (moduleName in params.installedAssetModules) return@forEach

                listBundleData.add(sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName, null, null))

                packageDownloadData[packageName]?.getModuleData(moduleName)?.chunks?.forEach { chunkData ->
                    val destination = chunkData.getChunkFile(context)
                    if (destination.exists() && destination.length() == chunkData.chunkBytesToDownload) {
                        sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, moduleName, chunkData, destination)
                    }
                }
            }
        }

        Log.d(TAG, "getSessionStates: $listBundleData")
        callback?.onGetSessionStates(listBundleData)
    }

    override suspend fun notifyChunkTransferred(params: NotifyChunkTransferredParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        checkSessionValid(packageName, params.sessionId)

        synchronized(lock) {
            val downLoadFile = context.getChunkFile(params.sessionId, params.moduleName, params.sliceId, params.chunkNumber)
            fileDescriptorMap[downLoadFile]?.close()
            fileDescriptorMap.remove(downLoadFile)
        }
        // TODO: Remove chunk after successful transfer of chunk or only with module?
        callback?.onNotifyChunkTransferred(
            bundleOf(BundleKeys.MODULE_NAME to params.moduleName) +
                    (BundleKeys.SLICE_ID to params.sliceId) +
                    (BundleKeys.CHUNK_NUMBER to params.chunkNumber) +
                    (BundleKeys.SESSION_ID to params.sessionId),
            bundleOf(BundleKeys.ERROR_CODE to AssetPackErrorCode.NO_ERROR)
        )
    }

    override suspend fun notifyModuleCompleted(params: NotifyModuleCompletedParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        checkSessionValid(packageName, params.sessionId)

        synchronized(lock) {
            packageDownloadData[packageName]?.updateDownloadStatus(params.moduleName, AssetPackStatus.COMPLETED)
            sendBroadcastForExistingFile(context, packageDownloadData[packageName]!!, params.moduleName, null, null)
        }

        val directory = context.getModuleDir(params.sessionId, params.moduleName)
        if (directory.exists()) {
            directory.deleteRecursively()
            Log.d(TAG, "Directory $directory deleted successfully.")
        } else {
            Log.d(TAG, "Directory $directory does not exist.")
        }
        callback?.onNotifyModuleCompleted(
            bundleOf(BundleKeys.MODULE_NAME to params.moduleName) +
                    (BundleKeys.SESSION_ID to params.sessionId),
            bundleOf(BundleKeys.ERROR_CODE to AssetPackErrorCode.NO_ERROR)
        )
    }

    override suspend fun notifySessionFailed(params: NotifySessionFailedParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        checkSessionValid(packageName, params.sessionId)

        // TODO: Implement
        callback?.onNotifySessionFailed(bundleOf(BundleKeys.SESSION_ID to params.sessionId))
        //throw UnsupportedOperationException()
    }

    override suspend fun keepAlive(params: KeepAliveParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        // TODO: Implement
        // Not throwing an exception is the better fallback implementation
    }

    override suspend fun getChunkFileDescriptor(params: GetChunkFileDescriptorParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        checkSessionValid(packageName, params.sessionId)

        val parcelFileDescriptor = synchronized(lock) {
            val downLoadFile = context.getChunkFile(params.sessionId, params.moduleName, params.sliceId, params.chunkNumber)
            ParcelFileDescriptor.open(downLoadFile, ParcelFileDescriptor.MODE_READ_ONLY).also {
                fileDescriptorMap[downLoadFile] = it
            }
        }

        Log.d(TAG, "getChunkFileDescriptor -> $parcelFileDescriptor")
        callback?.onGetChunkFileDescriptor(
            bundleOf(BundleKeys.CHUNK_FILE_DESCRIPTOR to parcelFileDescriptor),
            bundleOf(BundleKeys.ERROR_CODE to AssetPackErrorCode.NO_ERROR)
        )
    }

    override suspend fun requestDownloadInfo(params: RequestDownloadInfoParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        val needInit = synchronized(lock) {
            packageDownloadData[packageName] == null ||
                packageDownloadData[packageName]?.packageName != packageName ||
                packageDownloadData[packageName]?.moduleNames?.intersect(params.moduleNames.toSet())?.isEmpty() == true
        }

        if (needInit) {
            val newData = httpClient.initAssetModuleData(context, packageName, accountManager, params.moduleNames, params.options)
            synchronized(lock) {
                packageDownloadData[packageName] = packageDownloadData[packageName].merge(newData)
            }
            if (packageDownloadData[packageName] == null) {
                throw AssetPackException(AssetPackErrorCode.API_NOT_AVAILABLE)
            }
        }
        if (packageDownloadData[packageName] != null && packageDownloadData[packageName]?.moduleNames?.all {
                packageDownloadData[packageName]?.getModuleData(it)?.status == AssetPackStatus.COMPLETED
            } == true && params.installedAssetModules.isEmpty()) {
            Log.d(TAG, "requestDownloadInfo: resetAllModuleStatus ")
            packageDownloadData[packageName]?.resetAllModuleStatus()
        }
        params.moduleNames.forEach {
            val packData = packageDownloadData[packageName]?.getModuleData(it)
            if (packData?.status == AssetPackStatus.FAILED) {
                // FIXME: If we start download later, we shouldn't send a failure callback now
                callback?.onError(Bundle().apply { put(BundleKeys.ERROR_CODE, AssetPackErrorCode.NETWORK_ERROR) })
            }
        }
        val bundleData = buildDownloadBundle(packageDownloadData[packageName]!!, params.moduleNames)
        Log.d(TAG, "requestDownloadInfo -> $bundleData")
        callback?.onRequestDownloadInfo(bundleData, bundleData)
    }

    override suspend fun removeModule(params: RemoveModuleParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        checkSessionValid(packageName, params.sessionId)
        // TODO: Implement
        throw UnsupportedOperationException()
    }

    override suspend fun cancelDownloads(params: CancelDownloadsParameters, packageName: String, callback: IAssetModuleServiceCallback?) {
        // TODO: Implement
        callback?.onCancelDownloads(bundleOf(BundleKeys.ERROR_CODE to AssetPackErrorCode.NO_ERROR))
        //throw UnsupportedOperationException()
    }
}