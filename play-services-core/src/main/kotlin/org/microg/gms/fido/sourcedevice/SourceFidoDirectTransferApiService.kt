/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.sourcedevice

import android.content.Context
import android.os.ParcelFileDescriptor
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fido.sourcedevice.SourceStartDirectTransferOptions
import com.google.android.gms.fido.sourcedevice.internal.IStartDirectTransferCallbacks
import com.google.android.gms.fido.sourcedevice.internal.ISourceDirectTransferService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GmsService.FIDO_SOURCE_DIRECT_TRANSFER
import org.microg.gms.common.PackageUtils
import org.microg.gms.common.Constants
import org.microg.gms.utils.ExtendedPackageInfo

private const val TAG = "SourceFidoTransfer"

class SourceFidoDirectTransferApiService : BaseService(TAG, FIDO_SOURCE_DIRECT_TRANSFER) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val callerPackage = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitComplete(
            CommonStatusCodes.SUCCESS,
            SourceFidoDirectTransferServiceImpl(applicationContext, callerPackage).asBinder(),
            null,
        )
    }
}

class SourceFidoDirectTransferServiceImpl(
    private val context: Context,
    private val callerPackage: String,
) : ISourceDirectTransferService.Stub() {
    override fun startDirectTransfer(
        callbacks: IStartDirectTransferCallbacks,
        options: SourceStartDirectTransferOptions,
        input: ParcelFileDescriptor,
        output: ParcelFileDescriptor,
    ) {
        closePipes(input, output)
        callbacks.onResult(Status.INTERNAL_ERROR, null, ApiMetadata.DEFAULT)
    }

    override fun startDirectTransferV2(
        callback: IStatusCallback,
        options: SourceStartDirectTransferOptions,
        input: ParcelFileDescriptor,
        output: ParcelFileDescriptor,
    ) {
        if (!isAllowedDirectCaller(options)) {
            closePipes(input, output)
            callback.onResult(Status.INTERNAL_ERROR)
            return
        }
        start(options, input, output, callback::onResult)
    }

    private fun isAllowedDirectCaller(options: SourceStartDirectTransferOptions): Boolean =
        options.callerType == SourceStartDirectTransferOptions.CALLER_TYPE_SOURCE_DEVICE &&
            callerPackage == Constants.GMS_PACKAGE_NAME &&
            ExtendedPackageInfo(context, callerPackage).isGoogleOrPlatformPackage

    private fun closePipes(input: ParcelFileDescriptor, output: ParcelFileDescriptor) {
        runCatching { input.close() }
        runCatching { output.close() }
    }

    private fun start(
        options: SourceStartDirectTransferOptions,
        input: ParcelFileDescriptor,
        output: ParcelFileDescriptor,
        callback: (Status) -> Unit,
    ) {
        SourceFidoDirectTransferOperation(context, options, input, output).start(callback)
    }
}
