/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.inappreviewservice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.google.android.play.core.inappreview.protocol.IInAppReviewService
import com.google.android.play.core.inappreview.protocol.IInAppReviewServiceCallback
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "InAppReviewService"

class InAppReviewService  : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        return InAppReviewServiceImpl(this).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }
}

class InAppReviewServiceImpl(val context: Context) : IInAppReviewService.Stub() {

    override fun requestInAppReview(packageName: String?, bundle: Bundle?, callback: IInAppReviewServiceCallback?) {
        bundle?.keySet()
        Log.d(TAG, "requestInAppReview: packageName: $packageName bundle:$bundle")
        callback?.onResult(Bundle.EMPTY)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int) =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}