/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.blockstore

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.blockstore.AppRestoreInfo
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.google.android.gms.auth.blockstore.internal.IBlockstoreService
import com.google.android.gms.auth.blockstore.internal.IDeleteBytesCallback
import com.google.android.gms.auth.blockstore.internal.IGetAccessForPackageCallback
import com.google.android.gms.auth.blockstore.internal.IGetBlockstoreDataCallback
import com.google.android.gms.auth.blockstore.internal.IIsEndToEndEncryptionAvailableCallback
import com.google.android.gms.auth.blockstore.internal.IRetrieveBytesCallback
import com.google.android.gms.auth.blockstore.internal.ISetBlockstoreDataCallback
import com.google.android.gms.auth.blockstore.internal.IStoreBytesCallback
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GmsService.BLOCK_STORE
import org.microg.gms.common.PackageUtils

private const val TAG = "BlockstoreApiService"

private val FEATURES = arrayOf(
    Feature("auth_blockstore", 3),
    Feature("blockstore_data_transfer", 1),
    Feature("blockstore_notify_app_restore", 1),
    Feature("blockstore_store_bytes_with_options", 2),
    Feature("blockstore_is_end_to_end_encryption_available", 1),
    Feature("blockstore_enable_cloud_backup", 1),
    Feature("blockstore_delete_bytes", 2),
    Feature("blockstore_retrieve_bytes_with_options", 3),
    Feature("auth_clear_restore_credential", 2),
    Feature("auth_create_restore_credential", 1),
    Feature("auth_get_restore_credential", 1),
    Feature("auth_get_private_restore_credential_key", 1),
    Feature("auth_set_private_restore_credential_key", 1),
)

class BlockstoreApiService : BaseService(TAG, BLOCK_STORE) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        try {
            val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName) ?: throw IllegalArgumentException("Missing package name")

            val blockStoreImpl = BlockStoreImpl(this, packageName)
            callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS, BlobstoreServiceImpl(blockStoreImpl, lifecycle).asBinder(), ConnectionInfo().apply { features = FEATURES })
        } catch (e: Exception) {
            Log.w(TAG, "handleServiceRequest", e)
            callback.onPostInitComplete(CommonStatusCodes.INTERNAL_ERROR, null, null)
        }
    }
}

class BlobstoreServiceImpl(val blockStore: BlockStoreImpl, override val lifecycle: Lifecycle) : IBlockstoreService.Stub(), LifecycleOwner {

    override fun retrieveBytes(callback: IRetrieveBytesCallback?) {
        Log.d(TAG, "Method (retrieveBytes) called")
        lifecycleScope.launch {
            runCatching {
                val retrieveBytes = blockStore.retrieveBytes()
                if (retrieveBytes != null) {
                    callback?.onBytesResult(Status.SUCCESS, retrieveBytes)
                } else {
                    callback?.onBytesResult(Status.INTERNAL_ERROR, null)
                }
            }
        }
    }

    override fun setBlockstoreData(callback: ISetBlockstoreDataCallback?, data: ByteArray?) {
        Log.d(TAG, "Method (setBlockstoreData: ${data?.size}) called but not implemented")
    }

    override fun getBlockstoreData(callback: IGetBlockstoreDataCallback?) {
        Log.d(TAG, "Method (getBlockstoreData) called but not implemented")
    }

    override fun getAccessForPackage(callback: IGetAccessForPackageCallback?, packageName: String?) {
        Log.d(TAG, "Method (getAccessForPackage: $packageName) called but not implemented")
    }

    override fun setFlagWithPackage(callback: IStatusCallback?, packageName: String?, flag: Int) {
        Log.d(TAG, "Method (setFlagWithPackage: $packageName, $flag) called but not implemented")
    }

    override fun clearFlagForPackage(callback: IStatusCallback?, packageName: String?) {
        Log.d(TAG, "Method (clearFlagForPackage: $packageName) called but not implemented")
    }

    override fun updateFlagForPackage(callback: IStatusCallback?, packageName: String?, value: Int) {
        Log.d(TAG, "Method (updateFlagForPackage: $packageName, $value) called but not implemented")
    }

    override fun reportAppRestore(callback: IStatusCallback?, packages: List<String?>?, code: Int, info: AppRestoreInfo?) {
        Log.d(TAG, "Method (reportAppRestore: $packages, $code, $info) called but not implemented")
    }

    override fun storeBytes(callback: IStoreBytesCallback?, data: StoreBytesData?) {
        Log.d(TAG, "Method (storeBytes: $data) called")
        lifecycleScope.launch {
            runCatching {
                val storeBytes = blockStore.storeBytes(data)
                if (storeBytes != 0) {
                    callback?.onStoreBytesResult(Status.SUCCESS, storeBytes)
                } else {
                    callback?.onStoreBytesResult(Status.INTERNAL_ERROR, 0)
                }
            }
        }
    }

    override fun isEndToEndEncryptionAvailable(callback: IIsEndToEndEncryptionAvailableCallback?) {
        Log.d(TAG, "Method (isEndToEndEncryptionAvailable) called")
        runCatching { callback?.onCheckEndToEndEncryptionResult(Status.SUCCESS, false) }
    }

    override fun retrieveBytesWithRequest(callback: IRetrieveBytesCallback?, request: RetrieveBytesRequest?) {
        Log.d(TAG, "Method (retrieveBytesWithRequest: $request) called")
        lifecycleScope.launch {
            runCatching {
                val retrieveBytesResponse = blockStore.retrieveBytesWithRequest(request)
                Log.d(TAG, "retrieveBytesWithRequest: retrieveBytesResponse: $retrieveBytesResponse")
                if (retrieveBytesResponse != null) {
                    callback?.onResponseResult(Status.SUCCESS, retrieveBytesResponse)
                } else {
                    callback?.onResponseResult(Status.INTERNAL_ERROR, RetrieveBytesResponse(Bundle.EMPTY, emptyList()))
                }
            }
        }
    }

    override fun deleteBytes(callback: IDeleteBytesCallback?, request: DeleteBytesRequest?) {
        Log.d(TAG, "Method (deleteBytes: $request) called")
        lifecycleScope.launch {
            runCatching {
                val deleted = blockStore.deleteBytesWithRequest(request)
                callback?.onDeleteBytesResult(Status.SUCCESS, deleted)
            }
        }
    }
}