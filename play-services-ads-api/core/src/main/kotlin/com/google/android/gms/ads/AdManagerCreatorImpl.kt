/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.content.Context
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.ads.internal.client.AdSizeParcel
import com.google.android.gms.ads.internal.client.IAdManagerCreator
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AdManagerCreator"

@Keep
open class AdManagerCreatorImpl : IAdManagerCreator.Stub() {
    override fun newAdManager(context: IObjectWrapper?, adSize: AdSizeParcel?, adUnitId: String?, adapterCreator: IAdapterCreator?, clientVersion: Int): IBinder {
        Log.d(TAG, "newAdManager: adUnitId=$adUnitId clientVersion=$clientVersion")
        return LegacyAdManager(ObjectWrapper.unwrap(context) as? Context)
    }

    override fun newAdManagerByType(context: IObjectWrapper?, adSize: AdSizeParcel?, adUnitId: String?, adapterCreator: IAdapterCreator?, clientVersion: Int, type: Int): IBinder {
        Log.d(TAG, "newAdManagerByType: adUnitId=$adUnitId clientVersion=$clientVersion type=$type")
        return LegacyAdManager(ObjectWrapper.unwrap(context) as? Context)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
