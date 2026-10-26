/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.content.Context
import android.os.Parcel
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator
import com.google.android.gms.ads.internal.signals.ISignalGenerator
import com.google.android.gms.ads.internal.signals.ISignalGeneratorCreator
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import org.microg.gms.ads.signals.SignalGeneratorImpl
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AdsSignalCreator"

@Keep
class DynamiteSignalGeneratorCreatorImpl : ISignalGeneratorCreator.Stub() {
    override fun newSignalGenerator(context: IObjectWrapper?, adapterCreator: IAdapterCreator?, clientVersion: Int): ISignalGenerator {
        require(ObjectWrapper.unwrap(context) is Context) { "Invalid signal generator context" }
        Log.d(TAG, "create: clientVersion=$clientVersion adapterCreator=${adapterCreator != null}")
        return SignalGeneratorImpl().also { Log.d(TAG, "create_result: generator=ready") }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
