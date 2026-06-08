/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.util.Log
import com.google.android.gms.ads.formats.AdManagerAdViewOptions
import com.google.android.gms.ads.formats.PublisherAdViewOptions
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

internal class LegacyAdLoaderBuilder : IAdLoaderBuilder.Stub() {
    override fun build(): IAdLoader = LegacyAdLoader
    override fun setAdListener(listener: IAdListener?) = Unit
    override fun setAppInstallAdLoadedListener(listener: IOnAppInstallAdLoadedListener?) = Unit
    override fun setContentAdLoadedListener(listener: IOnContentAdLoadedListener?) = Unit
    override fun forCustomFormatAd(templateId: String?, onCustomFormatAdLoadedListener: IOnCustomTemplateAdLoadedListener?, onCustomClickListener: IOnCustomClickListener?) = Unit
    override fun setNativeAdOptions(options: NativeAdOptionsParcel?) = Unit
    override fun setCorrelationIdProvider(provider: ICorrelationIdProvider?) = Unit
    override fun forPublisherAdView(onPublisherAdViewLoadedListener: IOnPublisherAdViewLoadedListener?, adSize: AdSizeParcel?) = Unit
    override fun setPublisherAdViewOptions(options: PublisherAdViewOptions?) = Unit
    override fun setUnifiedNativeAdLoadedListener(listener: IOnUnifiedNativeAdLoadedListener?) = Unit
    override fun forInstreamAd(config: InstreamAdConfigurationParcel?) = Unit
    override fun setInstreamAdLoadCallback(callback: IInstreamAdLoadCallback?) = Unit
    override fun setAdManagerAdViewOptions(options: AdManagerAdViewOptions?) = Unit
}

private object LegacyAdLoader : IAdLoader.Stub() {
    override fun load(request: AdRequestParcel?) {
        Log.w(TAG, "load")
    }
    override fun getMediationAdapterClassName(): String? = null
    override fun isLoading(): Boolean = false
    override fun getAdManagerAdapterClassName(): String? = null
    override fun loadAds(request: AdRequestParcel?, count: Int) {
        Log.w(TAG, "loadAds")
    }
}
