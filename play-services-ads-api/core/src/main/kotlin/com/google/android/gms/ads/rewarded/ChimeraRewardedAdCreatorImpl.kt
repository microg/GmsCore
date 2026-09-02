/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads.rewarded

import android.content.Context
import android.os.IBinder
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator
import com.google.android.gms.ads.internal.rewarded.client.IRewardedAdCreator
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import org.microg.gms.ads.rewarded.RewardedAdImpl

private const val TAG = "RewardedAd"

@Keep
class ChimeraRewardedAdCreatorImpl : IRewardedAdCreator.Stub() {
    override fun newRewardedAd(context: IObjectWrapper, str: String, adapterCreator: IAdapterCreator, clientVersion: Int): IBinder {
        Log.d(TAG, "newRewardedAd($str, $clientVersion)")
        return RewardedAdImpl(ObjectWrapper.unwrap(context) as Context?, str, adapterCreator, clientVersion)
    }
}

