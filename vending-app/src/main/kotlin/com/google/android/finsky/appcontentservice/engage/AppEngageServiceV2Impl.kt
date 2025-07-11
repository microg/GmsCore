package com.google.android.finsky.appcontentservice.engage

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.engage.protocol.IAppEngageService
import com.google.android.engage.protocol.IAppEngageServiceAvailableCallback
import com.google.android.engage.protocol.IAppEngageServiceDeleteClustersCallback
import com.google.android.engage.protocol.IAppEngageServicePublishClustersCallback
import com.google.android.engage.protocol.IAppEngageServicePublishStatusCallback

class AppEngageServiceV2Impl(override val lifecycle: Lifecycle) : IAppEngageService.Stub(), LifecycleOwner {
    private val TAG = "AppEngageServiceV2Impl"
    override fun publishClusters(bundle: Bundle?, callback: IAppEngageServicePublishClustersCallback?) {
        Log.d(TAG, "publishClusters: ")
    }

    override fun deleteClusters(bundle: Bundle?, callback: IAppEngageServiceDeleteClustersCallback?) {
        Log.d(TAG, "deleteClusters: ")
    }

    override fun isServiceAvailable(bundle: Bundle?, callback: IAppEngageServiceAvailableCallback?) {
        Log.d(TAG, "isServiceAvailable: ")
        val result = Bundle()
        result.putBoolean("availability", false)
        callback?.onResult(result)
    }

    override fun updatePublishStatus(bundle: Bundle?, callback: IAppEngageServicePublishStatusCallback?) {
        Log.d(TAG, "updatePublishStatus: ")
    }
}