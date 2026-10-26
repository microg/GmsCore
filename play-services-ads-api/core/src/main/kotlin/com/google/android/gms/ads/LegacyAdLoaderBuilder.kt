/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.ads.formats.AdManagerAdViewOptions
import com.google.android.gms.ads.formats.PublisherAdViewOptions
import com.google.android.gms.ads.internal.AdErrorParcel
import com.google.android.gms.ads.internal.AdRequestParcel
import com.google.android.gms.ads.internal.client.AdSizeParcel
import com.google.android.gms.ads.internal.client.IAdListener
import com.google.android.gms.ads.internal.client.IAdLoader
import com.google.android.gms.ads.internal.client.IAdLoaderBuilder
import com.google.android.gms.ads.internal.client.ICorrelationIdProvider
import com.google.android.gms.ads.internal.formats.NativeAdOptionsParcel
import com.google.android.gms.ads.internal.formats.client.IOnAppInstallAdLoadedListener
import com.google.android.gms.ads.internal.formats.client.IOnContentAdLoadedListener
import com.google.android.gms.ads.internal.formats.client.IOnCustomClickListener
import com.google.android.gms.ads.internal.formats.client.IOnCustomTemplateAdLoadedListener
import com.google.android.gms.ads.internal.formats.client.IOnPublisherAdViewLoadedListener
import com.google.android.gms.ads.internal.formats.client.IOnUnifiedNativeAdLoadedListener
import com.google.android.gms.ads.internal.instream.InstreamAdConfigurationParcel
import com.google.android.gms.ads.internal.instream.client.IInstreamAdLoadCallback

private const val TAG = "LegacyAdLoader"

private fun createLoaderNoFillError() = AdErrorParcel().apply {
    code = 3
    message = "No fill"
    domain = "com.google.android.gms.ads"
}

internal class LegacyAdLoaderBuilder : IAdLoaderBuilder.Stub() {
    private var adListener: IAdListener? = null

    override fun build(): IAdLoader = LegacyAdLoader(adListener)
    override fun withAdListener(listener: IAdListener?) {
        adListener = listener
    }
    override fun withAppInstallAdLoadedListener(listener: IOnAppInstallAdLoadedListener?) = Unit
    override fun withContentAdLoadedListener(listener: IOnContentAdLoadedListener?) = Unit
    override fun forCustomFormatAd(templateId: String?, onCustomFormatAdLoadedListener: IOnCustomTemplateAdLoadedListener?, onCustomClickListener: IOnCustomClickListener?) = Unit
    override fun withNativeAdOptions(options: NativeAdOptionsParcel?) = Unit
    override fun withCorrelationIdProvider(provider: ICorrelationIdProvider?) = Unit
    override fun forAdView(onPublisherAdViewLoadedListener: IOnPublisherAdViewLoadedListener?, adSize: AdSizeParcel?) = Unit
    override fun withPublisherAdViewOptions(options: PublisherAdViewOptions?) = Unit
    override fun forNativeAd(listener: IOnUnifiedNativeAdLoadedListener?) = Unit
    override fun forInstreamAd(config: InstreamAdConfigurationParcel?) = Unit
    override fun setInstreamAdLoadCallback(callback: IInstreamAdLoadCallback?) = Unit
    override fun withAdManagerAdViewOptions(options: AdManagerAdViewOptions?) = Unit
}

private class LegacyAdLoader(private val adListener: IAdListener?) : IAdLoader.Stub() {
    private fun reportNoFill() {
        val listener = adListener ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                listener.onAdFailedToLoadWithAdError(createLoaderNoFillError())
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to report no-fill", e)
            }
        }
    }

    override fun load(request: AdRequestParcel?) {
        Log.w(TAG, "load")
        reportNoFill()
    }
    override fun getMediationAdapterClassName(): String? = null
    override fun isLoading(): Boolean = false
    override fun getAdManagerAdapterClassName(): String? = null
    override fun loadAds(request: AdRequestParcel?, count: Int) {
        Log.w(TAG, "loadAds")
        reportNoFill()
    }
}
