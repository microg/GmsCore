/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ads.rewarded

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.ads.internal.AdErrorParcel
import com.google.android.gms.ads.internal.AdRequestParcel
import com.google.android.gms.ads.internal.ServerSideVerificationOptionsParcel
import com.google.android.gms.ads.internal.client.IOnAdMetadataChangedListener
import com.google.android.gms.ads.internal.client.IOnPaidEventListener
import com.google.android.gms.ads.internal.client.IResponseInfo
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator
import com.google.android.gms.ads.internal.rewarded.client.*
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.dynamic.IObjectWrapper

private const val TAG = "RewardedAd"

class RewardedAdImpl(context: Context?, str: String?, adapterCreator: IAdapterCreator?, clientVersion: Int) : IRewardedAd.Stub() {
    private var immersive: Boolean = false

    private fun load(request: AdRequestParcel, callback: IRewardedAdLoadCallback, interstitial: Boolean) {
        Handler(Looper.getMainLooper()).post {
            try {
                callback.onAdLoadError(AdErrorParcel().apply { code = CommonStatusCodes.INTERNAL_ERROR; message = "Not supported" })
            } catch (e: RemoteException) {
                Log.w(TAG, e)
            }
        }
    }

    override fun load(request: AdRequestParcel, callback: IRewardedAdLoadCallback) {
        Log.d(TAG, "load")
        load(request, callback, false)
    }

    override fun setCallback(callback: IRewardedAdCallback) {
        Log.d(TAG, "setCallback")
    }

    override fun canBeShown(): Boolean {
        Log.d(TAG, "canBeShown")
        return false
    }

    override fun getMediationAdapterClassName(): String {
        Log.d(TAG, "getMediationAdapterClassName")
        return responseInfo.mediationAdapterClassName
    }

    override fun show(activity: IObjectWrapper) {
        Log.d(TAG, "show")
        showWithImmersive(activity, immersive)
    }

    override fun setRewardedAdSkuListener(listener: IRewardedAdSkuListener?) {
        Log.d(TAG, "setRewardedAdSkuListener")
    }

    override fun setServerSideVerificationOptions(options: ServerSideVerificationOptionsParcel) {
        Log.d(TAG, "setServerSideVerificationOptions")
    }

    override fun setOnAdMetadataChangedListener(listener: IOnAdMetadataChangedListener) {
        Log.d(TAG, "setOnAdMetadataChangedListener")
    }

    override fun getAdMetadata(): Bundle {
        Log.d(TAG, "getAdMetadata")
        return Bundle()
    }

    override fun showWithImmersive(activity: IObjectWrapper?, immersive: Boolean) {
        Log.d(TAG, "showWithBoolean")
    }

    override fun getRewardItem(): IRewardItem? {
        Log.d(TAG, "getRewardItem")
        return null
    }

    override fun getResponseInfo(): IResponseInfo {
        Log.d(TAG, "getResponseInfo")
        return ResponseInfoImpl()
    }

    override fun setOnPaidEventListener(listener: IOnPaidEventListener) {
        Log.d(TAG, "setOnPaidEventListener")
    }

    override fun loadInterstitial(request: AdRequestParcel, callback: IRewardedAdLoadCallback) {
        Log.d(TAG, "loadInterstitial")
        load(request, callback, true)
    }

    override fun setImmersiveMode(enabled: Boolean) {
        Log.d(TAG, "setImmersiveMode($enabled)")
    }
}

