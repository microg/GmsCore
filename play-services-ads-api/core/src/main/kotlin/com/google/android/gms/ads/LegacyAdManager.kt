/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.internal.AdErrorParcel
import com.google.android.gms.ads.internal.AdRequestParcel
import com.google.android.gms.ads.internal.appopen.client.IAppOpenAdLoadCallback
import com.google.android.gms.ads.internal.client.AdSizeParcel
import com.google.android.gms.ads.internal.client.IAdClickListener
import com.google.android.gms.ads.internal.client.IAdListener
import com.google.android.gms.ads.internal.client.IAdLoadCallback
import com.google.android.gms.ads.internal.client.IAdManager
import com.google.android.gms.ads.internal.client.IAppEventListener
import com.google.android.gms.ads.internal.client.IFullScreenContentCallback
import com.google.android.gms.ads.internal.client.IOnPaidEventListener
import com.google.android.gms.ads.internal.client.IVideoController
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper

private const val TAG = "LegacyAdManager"

private fun createManagerNoFillError() = AdErrorParcel().apply {
    code = 3
    message = "No fill"
    domain = "com.google.android.gms.ads"
}

/**
 * Minimal [IAdManager] returned by [AdManagerCreatorImpl] for the banner/interstitial path.
 *
 * It is a non-null ad manager so the AdMob SDK keeps using microG's remote result. No ad is served:
 * [getView] returns an empty container, [loadAd] reports "no fill", and the listeners/getters are
 * accepted but inert.
 */
internal class LegacyAdManager(private val context: Context?) : IAdManager.Stub() {
    private val adView: FrameLayout? by lazy { context?.let { FrameLayout(it) } }
    private var adListener: IAdListener? = null
    private var appOpenAdLoadCallback: IAppOpenAdLoadCallback? = null

    private fun reportNoFill(listener: IAdListener?) {
        Handler(Looper.getMainLooper()).post {
            try {
                listener?.onAdFailedToLoadWithAdError(createManagerNoFillError())
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to report no-fill", e)
            }
        }
    }

    private fun reportAppOpenNoFill(callback: IAppOpenAdLoadCallback?) {
        Handler(Looper.getMainLooper()).post {
            try {
                callback?.onAdFailedToLoad(createManagerNoFillError())
                Log.w(TAG, "Delivered app-open no-fill callback")
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to report app-open no-fill", e)
            }
        }
    }

    override fun getView(): IObjectWrapper = ObjectWrapper.wrap(adView)
    override fun destroy() = Unit
    override fun loadAd(request: AdRequestParcel?): Boolean {
        Log.w(TAG, "loadAd: adListener=${adListener != null} appOpenCallback=${appOpenAdLoadCallback != null}")
        reportAppOpenNoFill(appOpenAdLoadCallback)
        reportNoFill(adListener)
        return false
    }
    override fun pause() = Unit
    override fun resume() = Unit
    override fun setAdListener(listener: IAdListener?) {
        adListener = listener
    }
    override fun setAppEventListener(listener: IAppEventListener?) = Unit
    override fun getAdSize(): AdSizeParcel? = null
    override fun setAdSize(adSize: AdSizeParcel?) = Unit
    override fun setAdClickListener(listener: IAdClickListener?) = Unit
    override fun setManualImpressionFlag(flag: Int) = Unit
    override fun getVideoController(): IVideoController? = null
    override fun setManualImpressionsEnabled(enabled: Boolean) = Unit
    override fun setAppOpenAdLoadCallback(callback: IAppOpenAdLoadCallback?) {
        Log.w(TAG, "setAppOpenAdLoadCallback: callback=${callback != null}")
        appOpenAdLoadCallback = callback
    }
    override fun setOnPaidEventListener(listener: IOnPaidEventListener?) = Unit
    override fun loadAdWithCallback(request: AdRequestParcel?, callback: IAdLoadCallback?) {
        Log.w(TAG, "loadAdWithCallback: callback=${callback != null}")
        callback ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                callback.onAdFailedToLoad(createManagerNoFillError())
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to report callback no-fill", e)
            }
        }
    }
    override fun showInterstitial(activityWrapper: IObjectWrapper?) = Unit
    override fun setFullScreenContentCallback(callback: IFullScreenContentCallback?) = Unit
    override fun setStartTimestampMillis(timestampMillis: Long) = Unit
}
