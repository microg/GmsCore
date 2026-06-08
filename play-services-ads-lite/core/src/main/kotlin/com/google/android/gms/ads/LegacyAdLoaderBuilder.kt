/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.os.IBinder
import android.util.Log
import com.google.android.gms.ads.internal.AdRequestParcel
import com.google.android.gms.ads.internal.client.IAdLoader
import com.google.android.gms.ads.internal.client.IAdLoaderBuilder

private const val TAG = "LegacyAdLoader"

internal class LegacyAdLoaderBuilder : IAdLoaderBuilder.Stub() {
    override fun build(): IAdLoader = LegacyAdLoader
    override fun setAdListener(listener: IBinder?) = Unit
    override fun setUnifiedNativeAdLoadedListener(listener: IBinder?) = Unit
}

private object LegacyAdLoader : IAdLoader.Stub() {
    override fun load(request: AdRequestParcel?) {
        Log.w(TAG, "load")
    }
    override fun isLoading(): Boolean = false
    override fun loadAds(request: AdRequestParcel?, count: Int) {
        Log.w(TAG, "load ads")
    }
}
