/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.installservice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.google.android.play.core.appupdate.protocol.IAppUpdateService
import com.google.android.play.core.appupdate.protocol.IAppUpdateServiceCallback
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "TriggeredUpdateService"

class DevTriggeredUpdateService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        return DevTriggeredUpdateServiceImpl(this, lifecycle).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }
}

class DevTriggeredUpdateServiceImpl(private val context: Context, override val lifecycle: Lifecycle) : IAppUpdateService.Stub(), LifecycleOwner {

    override fun requestUpdateInfo(packageName: String?, bundle: Bundle?, callback: IAppUpdateServiceCallback?) {
        bundle?.keySet()
        Log.d(TAG, "requestUpdateInfo: packageName: $packageName bundle: $bundle")
        callback?.onUpdateResult(bundleOf("error.code" to 0, "update.availability" to 1))
    }

    override fun completeUpdate(packageName: String?, bundle: Bundle?, callback: IAppUpdateServiceCallback?) {
        bundle?.keySet()
        Log.d(TAG, "completeUpdate: packageName: $packageName bundle: $bundle")
        callback?.onCompleteResult(bundleOf("error.code" to 0))
    }

    override fun updateProgress(bundle: Bundle?) {
        Log.d(TAG, "updateProgress:  bundle: $bundle")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}