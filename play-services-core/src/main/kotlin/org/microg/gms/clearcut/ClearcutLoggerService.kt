/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.clearcut

import android.os.Parcel
import android.os.RemoteException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.clearcut.LogEventParcelable
import com.google.android.gms.clearcut.internal.IClearcutLoggerCallbacks
import com.google.android.gms.clearcut.internal.IClearcutLoggerService
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "ClearcutLoggerService"
private const val COLLECT_FOR_DEBUG_DURATION = 24L * 60 * 60 * 1000

class ClearcutLoggerService : BaseService(TAG, GmsService.CLEARCUT_LOGGER) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(0, ClearcutLoggerServiceImpl(lifecycle), null)
    }
}

class ClearcutLoggerServiceImpl(private val lifecycle: Lifecycle) : IClearcutLoggerService.Stub(), LifecycleOwner {
    private var collectForDebugExpiryTime: Long = 0

    override fun log(callbacks: IClearcutLoggerCallbacks, event: LogEventParcelable) {
        lifecycleScope.launchWhenStarted {
            callbacks.onLogResult(Status.SUCCESS)
        }
    }

    override fun forceUpload(callbacks: IClearcutLoggerCallbacks) {
        lifecycleScope.launchWhenStarted {
            callbacks.onLogResult(Status.SUCCESS)
        }
    }

    override fun startCollectForDebug(callbacks: IClearcutLoggerCallbacks) {
        lifecycleScope.launchWhenStarted {
            collectForDebugExpiryTime = System.currentTimeMillis() + COLLECT_FOR_DEBUG_DURATION
            callbacks.onStartCollectForDebugResult(Status.SUCCESS, collectForDebugExpiryTime)
        }
    }

    override fun stopCollectForDebug(callbacks: IClearcutLoggerCallbacks) {
        lifecycleScope.launchWhenStarted {
            callbacks.onStopCollectForDebugResult(Status.SUCCESS)
        }
    }

    override fun getCollectForDebugExpiryTime(callbacks: IClearcutLoggerCallbacks) {
        lifecycleScope.launchWhenStarted {
            callbacks.onCollectForDebugExpiryTime(Status.SUCCESS, collectForDebugExpiryTime)
        }
    }

    override fun getLogEventParcelablesLegacy(callbacks: IClearcutLoggerCallbacks) {
        getLogEventParcelables(callbacks)
    }

    override fun getLogEventParcelables(callbacks: IClearcutLoggerCallbacks) {
        lifecycleScope.launchWhenStarted {
            callbacks.onLogEventParcelables(DataHolder.empty(CommonStatusCodes.SUCCESS))
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}
