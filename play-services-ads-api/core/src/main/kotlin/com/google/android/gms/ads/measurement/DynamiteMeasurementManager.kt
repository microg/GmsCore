/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads.measurement

import android.os.Parcel
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.dynamic.IObjectWrapper
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "DynamiteMeasurement"

@Keep
class DynamiteMeasurementManager : IMeasurementManager.Stub() {

    override fun initialize(context: IObjectWrapper?, proxy: IAppMeasurementProxy?) {
        Log.d(TAG, "Not yet implemented: initialize")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
