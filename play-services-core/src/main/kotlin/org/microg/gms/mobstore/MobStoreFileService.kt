/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.mobstore

import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.mobstore.DeleteFileRequest
import com.google.android.gms.mobstore.IMobStoreFileCallbacks
import com.google.android.gms.mobstore.IMobStoreFileService
import com.google.android.gms.mobstore.OpenFileDescriptorRequest
import com.google.android.gms.mobstore.RenameRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "MobStoreFileService"

class MobStoreFileService : BaseService(TAG, GmsService.MOBSTORE) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest start, packageName:${request.packageName}")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, MobStoreFileServiceImpl(request.packageName),
            ConnectionInfo().apply {
                features = arrayOf(
                    Feature("mdh_read_sync_status", 1),
                    Feature("mdh_time_series_write", 1),
                    Feature("mdh_broadcast_listeners", 1),
                    Feature("mdd_download_right_now", 1),
                    Feature("mdd_delayed_download", 1),
                    Feature("mobstore_write_api", 1),
                    Feature("mobstore_rename", 1),
                    Feature("icing_get_document", 1)
                )
            })
    }
}

class MobStoreFileServiceImpl(private val packageName: String) : IMobStoreFileService.Stub() {

    override fun openFile(callback: IMobStoreFileCallbacks, openFileDescriptorRequest: OpenFileDescriptorRequest) {
        Log.d(TAG, "call method openFile, packageName:$packageName")
        callback.onOpenFileResult(Status(CommonStatusCodes.DEVELOPER_ERROR, "unimplemented"), null)
    }

    override fun deleteFile(callback: IMobStoreFileCallbacks, deleteFileRequest: DeleteFileRequest) {
        Log.d(TAG, "call method deleteFile, packageName:$packageName")
        callback.onDeleteFileResult(Status(CommonStatusCodes.DEVELOPER_ERROR, "unimplemented"))
    }

    override fun renameFile(callback: IMobStoreFileCallbacks, renameRequest: RenameRequest) {
        Log.d(TAG, "call method renameFile, packageName:$packageName")
        callback.onRenameFileResult(Status(CommonStatusCodes.DEVELOPER_ERROR, "unimplemented"))
    }
}
