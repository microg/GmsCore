/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.assetmoduleservice

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.vending.VendingPreferences
import com.google.android.finsky.AssetModuleOptions
import com.google.android.finsky.bundleOf
import com.google.android.finsky.get
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.protocol.BundleKeys
import com.google.android.play.core.assetpacks.protocol.IAssetModuleService
import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

class AssetPackException(val code: @AssetPackErrorCode Int, message: String? = null) : Exception(message ?: "AssetPackException(code=$code)")

data class StartDownloadParameters(val moduleNames: List<String>, val installedAssetModules: Map<String, Long>, val options: AssetModuleOptions)
data class GetSessionStatesParameters(val installedAssetModules: Map<String, Long>, val options: AssetModuleOptions)
data class NotifyModuleCompletedParameters(val moduleName: String, val sessionId: Int, val options: AssetModuleOptions)
data class NotifySessionFailedParameters(val sessionId: Int, val options: AssetModuleOptions)
data class RequestDownloadInfoParameters(val moduleNames: List<String>, val installedAssetModules: Map<String, Long>, val options: AssetModuleOptions)
data class RemoveModuleParameters(val moduleName: String, val sessionId: Int, val options: AssetModuleOptions)
data class CancelDownloadsParameters(val moduleNames: List<String>, val options: AssetModuleOptions)
data class KeepAliveParameters(val options: AssetModuleOptions)

data class NotifyChunkTransferredParameters(
    val moduleName: String,
    val sliceId: String,
    val chunkNumber: Int,
    val sessionId: Int,
    val options: AssetModuleOptions
)

data class GetChunkFileDescriptorParameters(
    val moduleName: String,
    val sliceId: String,
    val chunkNumber: Int,
    val sessionId: Int,
    val options: AssetModuleOptions
)

abstract class AbstractAssetModuleServiceImpl(val context: Context, override val lifecycle: Lifecycle) : IAssetModuleService.Stub(), LifecycleOwner {
    private fun List<Bundle>.getModuleNames(): List<String> = mapNotNull { it.get(BundleKeys.MODULE_NAME).takeIf { !it.isNullOrBlank() } }
    private fun Bundle?.getInstalledAssetModules(): Map<String, Long> = get(BundleKeys.INSTALLED_ASSET_MODULE).orEmpty()
        .map { it.get(BundleKeys.INSTALLED_ASSET_MODULE_NAME) to it.get(BundleKeys.INSTALLED_ASSET_MODULE_VERSION) }
        .filter { it.first != null && it.second != null }
        .associate { it.first!! to it.second!! }

    private fun Bundle?.getOptions() = AssetModuleOptions(
        this.get(BundleKeys.PLAY_CORE_VERSION_CODE, 0),
        this.get(BundleKeys.SUPPORTED_COMPRESSION_FORMATS).orEmpty(),
        this.get(BundleKeys.SUPPORTED_PATCH_FORMATS).orEmpty(),
    )

    private fun sendError(callback: IAssetModuleServiceCallback?, method: String, errorCode: @AssetPackErrorCode Int) {
        Log.w(TAG, "Sending error from $method: $errorCode")
        callback?.onError(bundleOf(BundleKeys.ERROR_CODE to errorCode))
    }

    private fun <T: Any> run(
        uncheckedPackageName: String?,
        method: String,
        callback: IAssetModuleServiceCallback?,
        parseParameters: (packageName: String) -> T,
        logic: suspend (params: T, packageName: String, callback: IAssetModuleServiceCallback?) -> Unit
    ) {
        val packageName = try {
            PackageUtils.getAndCheckCallingPackage(context, uncheckedPackageName)!!
        } catch (e: Exception) {
            Log.w(TAG, e)
            return sendError(callback, method, AssetPackErrorCode.ACCESS_DENIED)
        }

        if (!VendingPreferences.isAssetDeliveryEnabled(context)) {
            return sendError(callback, method, AssetPackErrorCode.API_NOT_AVAILABLE)
        }

        val input = try {
            parseParameters(packageName)
        } catch (e: AssetPackException) {
            return sendError(callback, method, e.code)
        } catch (e: Exception) {
            Log.w(TAG, e)
            return sendError(callback, method, AssetPackErrorCode.INVALID_REQUEST)
        }

        Log.d(TAG, "$method[$packageName]${input.toString().substring(input.javaClass.simpleName.length)}")

        lifecycleScope.launchWhenStarted {
            try {
                logic.invoke(input, packageName, callback)
            } catch (e: AssetPackException) {
                sendError(callback, method, e.code)
            } catch (e: UnsupportedOperationException) {
                Log.w(TAG, "Unsupported: $method")
                sendError(callback, method, AssetPackErrorCode.API_NOT_AVAILABLE)
            } catch (e: Exception) {
                Log.w(TAG, e)
                sendError(callback, method, AssetPackErrorCode.INTERNAL_ERROR)
            }
        }
    }

    protected abstract fun getDefaultSessionId(packageName: String, moduleName: String): Int

    override fun startDownload(uncheckedPackageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "startDownload", callback, { _ ->
            StartDownloadParameters(list!!.getModuleNames().also { require(it.isNotEmpty()) }, bundle.getInstalledAssetModules(), bundle.getOptions())
        }, this::startDownload)
    }

    abstract suspend fun startDownload(params: StartDownloadParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun getSessionStates(uncheckedPackageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "getSessionStates", callback, { _ ->
            GetSessionStatesParameters(bundle.getInstalledAssetModules(), bundle.getOptions())
        }, this::getSessionStates)
    }

    abstract suspend fun getSessionStates(params: GetSessionStatesParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun notifyChunkTransferred(uncheckedPackageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "notifyChunkTransferred", callback, { packageName ->
            val moduleName = bundle.get(BundleKeys.MODULE_NAME)!!.also { require(it.isNotEmpty()) }
            NotifyChunkTransferredParameters(
                moduleName,
                bundle.get(BundleKeys.SLICE_ID)!!.also { require(it.isNotEmpty()) },
                bundle.get(BundleKeys.CHUNK_NUMBER, 0),
                bundle.get(BundleKeys.SESSION_ID, getDefaultSessionId(packageName, moduleName)),
                bundle2.getOptions()
            )
        }, this::notifyChunkTransferred)
    }

    abstract suspend fun notifyChunkTransferred(params: NotifyChunkTransferredParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun notifyModuleCompleted(uncheckedPackageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "notifyModuleCompleted", callback, { packageName ->
            val moduleName = bundle.get(BundleKeys.MODULE_NAME)!!.also { require(it.isNotEmpty()) }
            NotifyModuleCompletedParameters(
                moduleName,
                bundle.get(BundleKeys.SESSION_ID, getDefaultSessionId(packageName, moduleName)),
                bundle2.getOptions()
            )
        }, this::notifyModuleCompleted)
    }

    abstract suspend fun notifyModuleCompleted(params: NotifyModuleCompletedParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun notifySessionFailed(uncheckedPackageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "notifySessionFailed", callback, { _ ->
            NotifySessionFailedParameters(bundle.get(BundleKeys.SESSION_ID, 0), bundle2.getOptions())
        }, this::notifySessionFailed)
    }

    abstract suspend fun notifySessionFailed(params: NotifySessionFailedParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun getChunkFileDescriptor(uncheckedPackageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "getChunkFileDescriptor", callback, { packageName ->
            val moduleName = bundle.get(BundleKeys.MODULE_NAME)!!.also { require(it.isNotEmpty()) }
            GetChunkFileDescriptorParameters(
                moduleName,
                bundle.get(BundleKeys.SLICE_ID)!!,
                bundle.get(BundleKeys.CHUNK_NUMBER, 0),
                bundle.get(BundleKeys.SESSION_ID, getDefaultSessionId(packageName, moduleName)),
                bundle2.getOptions()
            )
        }, this::getChunkFileDescriptor)
    }

    abstract suspend fun getChunkFileDescriptor(params: GetChunkFileDescriptorParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun requestDownloadInfo(uncheckedPackageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "requestDownloadInfo", callback, { _ ->
            RequestDownloadInfoParameters(list!!.getModuleNames().also { require(it.isNotEmpty()) }, bundle.getInstalledAssetModules(), bundle.getOptions())
        }, this::requestDownloadInfo)
    }

    abstract suspend fun requestDownloadInfo(params: RequestDownloadInfoParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun removeModule(uncheckedPackageName: String?, bundle: Bundle?, bundle2: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "removeModule", callback, { packageName ->
            val moduleName = bundle?.get(BundleKeys.MODULE_NAME)!!
            RemoveModuleParameters(
                moduleName,
                bundle.get(BundleKeys.SESSION_ID, getDefaultSessionId(packageName, moduleName)),
                bundle2.getOptions()
            )
        }, this::removeModule)
    }

    abstract suspend fun removeModule(params: RemoveModuleParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun cancelDownloads(uncheckedPackageName: String?, list: MutableList<Bundle>?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(uncheckedPackageName, "cancelDownloads", callback, { _ ->
            CancelDownloadsParameters(list!!.getModuleNames().also { require(it.isNotEmpty()) }, bundle.getOptions())
        }, this::cancelDownloads)
    }

    abstract suspend fun cancelDownloads(params: CancelDownloadsParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun keepAlive(packageName: String?, bundle: Bundle?, callback: IAssetModuleServiceCallback?) {
        run(packageName, "keepAlive", callback, { KeepAliveParameters(bundle.getOptions()) }, this::keepAlive)
    }

    abstract suspend fun keepAlive(params: KeepAliveParameters, packageName: String, callback: IAssetModuleServiceCallback?)

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}