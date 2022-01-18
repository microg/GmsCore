/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.ConditionVariable
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import org.microg.gms.droidguard.GuardCallback
import java.io.FileNotFoundException

class DroidGuardHandleImpl(private val context: Context, private val packageName: String, private val factory: HandleProxyFactory, private val callback: GuardCallback) : IDroidGuardHandle.Stub() {
    private val condition = ConditionVariable()

    private var flow: String? = null
    private var handleProxy: HandleProxy? = null
    private var handleInitError: Throwable? = null

    override fun init(flow: String?) {
        Log.d(TAG, "init($flow)")
        initWithRequest(flow, null)
    }

    @SuppressLint("SetWorldReadable")
    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply {
        Log.d(TAG, "initWithRequest($flow)")
        this.flow = flow
        try {
            var handleProxy: HandleProxy? = null
            // FIXME: Temporary disabled low latency handle
//            if (flow !in NOT_LOW_LATENCY_FLOWS) {
//                try {
//                    handleProxy = factory.createLowLatencyHandle(flow, callback, request)
//                    Log.d(TAG, "Using low-latency handle")
//                } catch (e: Exception) {
//                    Log.w(TAG, e)
//                }
//            }
            if (handleProxy == null) {
                handleProxy = factory.createHandle(packageName, flow, callback, request)
            }
            if (handleProxy.init()) {
                this.handleProxy = handleProxy
            } else {
                throw Exception("init failed")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error during handle init", e)
            handleInitError = e
        }
        condition.open()
        if (handleInitError == null) {
            try {
                handleProxy?.let { handleProxy ->
                    val `object` = handleProxy.handle.javaClass.getDeclaredMethod("rb").invoke(handleProxy.handle) as? Parcelable?
                    if (`object` != null) {
                        val vmKey = handleProxy.vmKey
                        val theApk = factory.getTheApkFile(vmKey)
                        try {
                            theApk.setReadable(true, false)
                            return DroidGuardInitReply(ParcelFileDescriptor.open(theApk, ParcelFileDescriptor.MODE_READ_ONLY), `object`)
                        } catch (e: FileNotFoundException) {
                            throw Exception("Files for VM $vmKey not found on disk")
                        }
                    }
                }
            } catch (e: Exception) {
                this.handleProxy = null
                handleInitError = e
            }
        }
        return DroidGuardInitReply(null, null)
    }

    override fun guard(map: MutableMap<Any?, Any?>): ByteArray {
        Log.d(TAG, "guard()")
        handleInitError?.let { return FallbackCreator.create(flow, context, map, it) }
        val handleProxy = this.handleProxy ?: return FallbackCreator.create(flow, context, map, IllegalStateException())
        return try {
            handleProxy.handle::class.java.getDeclaredMethod("ss", Map::class.java).invoke(handleProxy.handle, map) as ByteArray
        } catch (e: Exception) {
            try {
                throw BytesException(handleProxy.extra, e)
            } catch (e2: Exception) {
                FallbackCreator.create(flow, context, map, e2)
            }
        }
    }

    override fun close() {
        Log.d(TAG, "close()")
        condition.block()
        try {
            handleProxy?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during handle close", e)
        }
        handleProxy = null
        handleInitError = null
    }

    companion object {
        private const val TAG = "GmsGuardHandleImpl"
        private val NOT_LOW_LATENCY_FLOWS = setOf("ad_attest", "attest", "checkin", "federatedMachineLearningReduced", "msa-f", "ad-event-attest-token")
    }
}
