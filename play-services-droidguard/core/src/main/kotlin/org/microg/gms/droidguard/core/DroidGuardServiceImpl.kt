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

class DroidGuardServiceImpl(private val service: DroidGuardChimeraService, private val packageName: String) : IDroidGuardService.Stub() {
    override fun guard(callbacks: IDroidGuardCallbacks?, flow: String?, map: MutableMap<Any?, Any?>?) {
        Log.d(TAG, "guard()")
        guardWithRequest(callbacks, flow, map, null)
    }

    override fun guardWithRequest(callbacks: IDroidGuardCallbacks?, flow: String?, map: MutableMap<Any?, Any?>?, request: DroidGuardResultsRequest?) {
        Log.d(TAG, "guardWithRequest()")
        TODO("Not yet implemented")
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
