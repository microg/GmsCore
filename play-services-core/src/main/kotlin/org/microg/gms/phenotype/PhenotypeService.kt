/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.phenotype.Configurations
import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GmsPhenotypeSvc"

class PhenotypeService : BaseService(TAG, GmsService.PHENOTYPE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        callback.onPostInitComplete(0, PhenotypeServiceImpl().asBinder(), null)
    }
}

class PhenotypeServiceImpl : IPhenotypeService.Stub() {
    override fun register(callbacks: IPhenotypeCallbacks, p1: String?, p2: Int, p3: Array<out String>?, p4: ByteArray?) {
        Log.d(TAG, "register($p1, $p2, $p3, $p4)")
        callbacks.onRegister(Status.SUCCESS)
    }

    override fun register2(callbacks: IPhenotypeCallbacks, p1: String?, p2: Int, p3: Array<out String>?, p4: IntArray?, p5: ByteArray?) {
        Log.d(TAG, "register2($p1, $p2, $p3, $p4, $p5)")
        callbacks.onRegister(Status.SUCCESS)
    }

    override fun getConfigurationSnapshot(callbacks: IPhenotypeCallbacks, p1: String?, p2: String?, p3: String?) {
        Log.d(TAG, "getConfigurationSnapshot($p1, $p2, $p3)")
        callbacks.onConfigurations(Status.SUCCESS, Configurations().apply {
            field4 = emptyArray()
        })
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}
