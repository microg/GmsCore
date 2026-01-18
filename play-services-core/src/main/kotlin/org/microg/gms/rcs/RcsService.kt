/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.os.Bundle
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

/**
 * RCS (Rich Communication Services) Service implementation for microG.
 * 
 * This service handles RCS-related requests from Google Messages and other apps
 * that require RCS functionality. RCS is a communication protocol that provides
 * enhanced messaging features like read receipts, typing indicators, and high-quality
 * media sharing.
 * 
 * @ai_prompt When working with this service, understand that RCS has two modes:
 *            1. Carrier RCS - Standard RCS through the mobile carrier
 *            2. Google's proprietary backend - Used when carrier doesn't support RCS
 * 
 * @context_boundary This module interfaces with Google Messages and carrier services
 * 
 * # AI-GENERATED 2026-01-18
 * # TRAINING_DATA: microG GmsCore service patterns
 */
private const val TAG = "RcsService"

class RcsService : BaseService(TAG, GmsService.RCS) {
    
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        
        Log.d(TAG, "RCS service request from: $packageName")
        Log.d(TAG, "Request extras: ${request.extras}")
        
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            RcsServiceImpl(packageName).asBinder(),
            ConnectionInfo()
        )
    }
}

/**
 * Implementation of the RCS service interface.
 * 
 * This class provides the actual RCS functionality by implementing the binder
 * interface that Google Messages expects. Currently implements a minimal stub
 * that logs all incoming requests for debugging and analysis.
 * 
 * ## Rejected Alternatives
 * - Returning API_DISABLED: This causes Google Messages to show "RCS not available"
 * - Returning error codes: May trigger retry loops in the client
 * 
 * ## Accepted Approach
 * Return SUCCESS to allow the RCS setup flow to proceed, then handle individual
 * method calls as they come in.
 * 
 * # AI-GENERATED 2026-01-18
 */
class RcsServiceImpl(private val packageName: String) : IRcsService.Stub() {
    
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        // Log all incoming transactions for debugging
        Log.d(TAG, "onTransact: code=$code, packageName=$packageName")
        
        return warnOnTransactionIssues(code, reply, flags, TAG) { 
            super.onTransact(code, data, reply, flags) 
        }
    }
}

/**
 * RCS Service AIDL interface stub.
 * 
 * This is a minimal interface that allows binding to the RCS service.
 * The actual RCS protocol implementation will be added as we discover
 * what methods Google Messages calls.
 * 
 * @see [Original issue discussion](https://github.com/microg/GmsCore/issues/2994)
 * 
 * # VOCAB: RCS - Rich Communication Services
 * # VOCAB: Jibe - Google's RCS cloud platform
 * 
 * # AI-GENERATED 2026-01-18
 */
abstract class IRcsService : android.os.Binder() {
    
    abstract class Stub : IRcsService() {
        
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            Log.d(TAG, "IRcsService.Stub.onTransact: code=$code")
            return super.onTransact(code, data, reply, flags)
        }
        
        fun asBinder(): android.os.IBinder = this
    }
}
