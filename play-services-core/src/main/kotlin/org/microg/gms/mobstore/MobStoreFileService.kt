/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.mobstore

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
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
import com.google.android.gms.mobstore.OpenFileDescriptorResponse
import com.google.android.gms.mobstore.RenameRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import java.io.File

private const val TAG = "MobStoreFileService"

class MobStoreFileService : BaseService(TAG, GmsService.MOBSTORE) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName) ?: throw IllegalArgumentException("Missing package name")
        Log.d(TAG, "handleServiceRequest start, packageName:$packageName")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, MobStoreFileServiceImpl(this, request.packageName), ConnectionInfo().apply {
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

class MobStoreFileServiceImpl(private val context: Context, private val packageName: String) : IMobStoreFileService.Stub() {

    override fun openFile(callback: IMobStoreFileCallbacks, openFileDescriptorRequest: OpenFileDescriptorRequest) {
        Log.d(TAG, "call method openFile, packageName:$packageName openFileDescriptorRequest:$openFileDescriptorRequest")
        try {
            val generateUrl = generateUrl(openFileDescriptorRequest.fileUri, context)
            try {
                callback.onOpenFileResult(
                    Status.SUCCESS, OpenFileDescriptorResponse(ParcelFileDescriptor.open(generateUrl, ParcelFileDescriptor.MODE_READ_WRITE))
                )
                Log.d(TAG, "call method openFile SUCCESS, url: $generateUrl")
            } catch (e: Exception) {
                if (generateUrl.exists()) {
                    throw Exception("Access denied to uri (but exists): ${openFileDescriptorRequest.fileUri}")
                }
                throw Exception("file not found: ${openFileDescriptorRequest.fileUri}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "openFile, but exception: ${e.localizedMessage}")
            callback.onOpenFileResult(Status(CommonStatusCodes.DEVELOPER_ERROR, e.localizedMessage), null)
        }
    }

    private fun generateUrl(uri: Uri, context: Context): File {
        if (!uri.scheme.equals("android")) {
            throw Exception("Scheme must be 'android'")
        }
        if (uri.pathSegments.isEmpty()) {
            throw Exception(String.format("Path must start with a valid logical location: %s", uri))
        }
        if (!TextUtils.isEmpty(uri.query)) {
            throw Exception("Did not expect uri to have query")
        }
        val pathSegments = uri.pathSegments
        val filesDir: File?
        when (pathSegments[0]) {
            "files" -> {
                filesDir = context.filesDir
            }

            "cache" -> {
                filesDir = context.cacheDir
            }

            "managed" -> {
                filesDir = File(context.filesDir, "managed")
            }

            "external" -> {
                filesDir = context.getExternalFilesDir(null)
            }

            else -> {
                throw Exception(String.format("Path must start with a valid logical location: %s", uri))
            }
        }
        return File(filesDir, TextUtils.join(File.separator, pathSegments.subList(1, pathSegments.size)))
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
