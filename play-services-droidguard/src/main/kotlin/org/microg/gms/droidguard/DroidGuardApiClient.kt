/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.os.IBinder
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import com.google.android.gms.droidguard.internal.IDroidGuardService
import org.microg.gms.common.GmsClient
import org.microg.gms.common.GmsService
import org.microg.gms.common.api.ConnectionCallbacks
import org.microg.gms.common.api.OnConnectionFailedListener

class DroidGuardApiClient(context: Context, connectionCallbacks: ConnectionCallbacks, onConnectionFailedListener: OnConnectionFailedListener) : GmsClient<IDroidGuardService>(context, connectionCallbacks, onConnectionFailedListener, GmsService.DROIDGUARD.ACTION) {
    init {
        serviceId = GmsService.DROIDGUARD.SERVICE_ID
    }

    override fun interfaceFromBinder(binder: IBinder): IDroidGuardService = IDroidGuardService.Stub.asInterface(binder)

    fun getHandle() = serviceInterface.handle
}
