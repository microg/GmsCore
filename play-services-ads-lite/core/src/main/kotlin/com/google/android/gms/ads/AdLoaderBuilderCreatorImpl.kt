/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.ads.internal.client.IAdLoaderBuilderCreator
import com.google.android.gms.ads.internal.meditation.client.IAdapterCreator
import com.google.android.gms.dynamic.IObjectWrapper
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AdLoaderBuilder"

@Keep
class AdLoaderBuilderCreatorImpl : IAdLoaderBuilderCreator.Stub() {
    override fun newAdLoaderBuilder(context: IObjectWrapper?, adUnitId: String, adapterCreator: IAdapterCreator?, clientVersion: Int): IBinder? {
        Log.d(TAG, "newAdLoaderBuilder: adUnitId=$adUnitId clientVersion=$clientVersion")
        return null
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
