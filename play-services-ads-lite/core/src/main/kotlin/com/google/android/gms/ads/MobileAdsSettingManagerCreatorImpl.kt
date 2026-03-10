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
import com.google.android.gms.ads.internal.client.IMobileAdsSettingManagerCreator
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import org.microg.gms.ads.MobileAdsSettingManagerImpl
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AdsSettingManager"

@Keep
class MobileAdsSettingManagerCreatorImpl : IMobileAdsSettingManagerCreator.Stub() {
    override fun getMobileAdsSettingManager(context: IObjectWrapper?, clientVersion: Int): IBinder {
        Log.d(TAG, "getMobileAdsSettingManager($clientVersion)")
        return MobileAdsSettingManagerImpl(ObjectWrapper.unwrap(context) as Context)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}

