/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.util.Log
import com.google.android.gms.droidguard.DroidGuardChimeraService
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardCallbacks
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import com.google.android.gms.droidguard.internal.IDroidGuardService
import java.util.concurrent.RejectedExecutionException

class DroidGuardServiceImpl(private val service: DroidGuardChimeraService, private val packageName: String) : IDroidGuardService.Stub() {
    override fun guard(callbacks: IDroidGuardCallbacks?, flow: String?, map: MutableMap<Any?, Any?>?) {
        Log.d(TAG, "guard()")
        guardWithRequest(callbacks, flow, map, null)
    }

    override fun guardWithRequest(callbacks: IDroidGuardCallbacks?, flow: String?, map: MutableMap<Any?, Any?>?, request: DroidGuardResultsRequest?) {
        Log.d(TAG, "guardWithRequest()")
        if (callbacks == null) return

        val requestMap = map?.let { HashMap(it) } ?: HashMap()
        val task = Runnable {
            var handle: IDroidGuardHandle? = null
            val result = try {
                handle = getHandle()
                val initReply = handle.initWithRequest(flow, request)
                if (initReply == null) handle.init(flow)
                else try {
                    initReply.pfd?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to close unused DroidGuard init descriptor", e)
                }
                handle.snapshot(HashMap(requestMap))
            } catch (e: Exception) {
                Log.w(TAG, "guardWithRequest failed", e)
                FallbackCreator.create(flow, service, requestMap, e)
            } finally {
                try {
                    handle?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to close DroidGuard handle", e)
                }
            }
            try {
                callbacks.onResult(result)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to deliver DroidGuard result", e)
            }
        }

        try {
            val executor = service.d
            if (executor != null) executor.execute(task) else task.run()
        } catch (e: RejectedExecutionException) {
            Log.w(TAG, "DroidGuard request queue is full", e)
            try {
                callbacks.onResult(FallbackCreator.create(flow, service, requestMap, e))
            } catch (callbackError: Exception) {
                Log.w(TAG, "Failed to deliver DroidGuard queue-rejection result", callbackError)
            }
        }
    }

    override fun getHandle(): IDroidGuardHandle {
        Log.d(TAG, "getHandle()")
        return when (DroidGuardPreferences.getMode(service)) {
            DroidGuardPreferences.Mode.Embedded -> DroidGuardHandleImpl(service, packageName, service.b, service.b(packageName))
            DroidGuardPreferences.Mode.Network -> RemoteHandleImpl(service, packageName)
        }
    }

    override fun getClientTimeoutMillis(): Int {
        Log.d(TAG, "getClientTimeoutMillis()")
        return 60000
    }

    companion object {
        const val TAG = "GmsGuardServiceImpl"
    }
}
