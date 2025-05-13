/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.threadnetwork

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.threadnetwork.ThreadBorderAgent
import com.google.android.gms.threadnetwork.ThreadNetworkCredentials
import com.google.android.gms.threadnetwork.ThreadNetworkStatusCodes
import com.google.android.gms.threadnetwork.internal.*
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "ThreadNetworkService"

class ThreadNetworkService : BaseService(TAG, GmsService.THREAD_NETWORK) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = arrayOf(Feature("threadnetwork", 8))
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS, ThreadNetworkServiceImpl(this, request.packageName).asBinder(), connectionInfo
        )
    }
}

class ThreadNetworkServiceImpl(private val context: Context, private val packageName: String) : IThreadNetworkService.Stub() {
    private val THREAD_NETWORK_NOT_FOUND = Status(ThreadNetworkStatusCodes.THREAD_NETWORK_NOT_FOUND, "THREAD_NETWORK_NOT_FOUND")

    override fun addCredentials(callback: IStatusCallback?, borderAgent: ThreadBorderAgent?, credentials: ThreadNetworkCredentials?) {
        if (borderAgent == null || credentials == null) {
            runCatching { callback?.onResult(Status(CommonStatusCodes.DEVELOPER_ERROR, "Illegal arguments")) }
            return
        }
        Log.d(TAG, "Not yet implemented: addCredentials")
        runCatching { callback?.onResult(Status.SUCCESS) }
    }

    override fun removeCredentials(callback: IStatusCallback?, borderAgent: ThreadBorderAgent?) {
        if (borderAgent == null) {
            runCatching { callback?.onResult(Status(CommonStatusCodes.DEVELOPER_ERROR, "Illegal arguments")) }
            return
        }
        Log.d(TAG, "Not yet implemented: removeCredentials")
        runCatching { callback?.onResult(Status.SUCCESS) }
    }

    override fun getAllCredentials(callbacks: IThreadNetworkServiceCallbacks?) {
        Log.d(TAG, "Not yet implemented: getAllCredentials")
        runCatching { callbacks?.onCredentials(Status.SUCCESS, emptyList()) }
    }

    override fun getCredentialsByExtendedPanId(callback: IGetCredentialsByExtendedPanIdCallback?, extendedPanId: ByteArray?) {
        if (extendedPanId == null) {
            runCatching { callback?.onCredentials(Status(CommonStatusCodes.DEVELOPER_ERROR, "Illegal arguments"), null) }
            return
        }
        Log.d(TAG, "Not yet implemented: getCredentialsByExtendedPanId")
        runCatching { callback?.onCredentials(THREAD_NETWORK_NOT_FOUND, null) }
    }

    override fun getCredentialsByBorderAgent(callbacks: IThreadNetworkServiceCallbacks?, borderAgent: ThreadBorderAgent?) {
        if (borderAgent == null) {
            runCatching { callbacks?.onCredentials(Status(CommonStatusCodes.DEVELOPER_ERROR, "Illegal arguments"), emptyList()) }
            return
        }
        Log.d(TAG, "Not yet implemented: getCredentialsByBorderAgent: $borderAgent")
        runCatching { callbacks?.onCredentials(Status.SUCCESS, emptyList()) }
    }

    override fun getPreferredCredentials(callback: IGetPreferredCredentialsCallback?) {
        Log.d(TAG, "Not yet implemented: getPreferredCredentials")
        runCatching { callback?.onPreferredCredentials(THREAD_NETWORK_NOT_FOUND, null) }
    }

    override fun isPreferredCredentials(callback: IIsPreferredCredentialsCallback?, credentials: ThreadNetworkCredentials?) {
        if (credentials == null) {
            runCatching { callback?.onIsPreferredCredentials(Status(CommonStatusCodes.DEVELOPER_ERROR, "Illegal arguments"), false) }
            return
        }
        Log.d(TAG, "Not yet implemented: isPreferredCredentials: $credentials")
        runCatching { callback?.onIsPreferredCredentials(THREAD_NETWORK_NOT_FOUND, false) }
    }

}