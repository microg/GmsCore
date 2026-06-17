/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.client.IAdLoader;
import com.google.android.gms.ads.internal.client.IAdListener;
import com.google.android.gms.ads.internal.client.ICorrelationIdProvider;
import com.google.android.gms.ads.internal.client.AdSizeParcel;
import com.google.android.gms.ads.internal.formats.NativeAdOptionsParcel;
import com.google.android.gms.ads.internal.formats.client.IOnAppInstallAdLoadedListener;
import com.google.android.gms.ads.internal.formats.client.IOnContentAdLoadedListener;
import com.google.android.gms.ads.internal.formats.client.IOnCustomTemplateAdLoadedListener;
import com.google.android.gms.ads.internal.formats.client.IOnCustomClickListener;
import com.google.android.gms.ads.internal.formats.client.IOnUnifiedNativeAdLoadedListener;
import com.google.android.gms.ads.internal.formats.client.IOnPublisherAdViewLoadedListener;
import com.google.android.gms.ads.internal.instream.InstreamAdConfigurationParcel;
import com.google.android.gms.ads.internal.instream.client.IInstreamAdLoadCallback;
import com.google.android.gms.ads.formats.PublisherAdViewOptions;
import com.google.android.gms.ads.formats.AdManagerAdViewOptions;

interface IAdLoaderBuilder {
    IAdLoader build() = 0;
    void withAdListener(IAdListener listener) = 1;
    void withAppInstallAdLoadedListener(IOnAppInstallAdLoadedListener listener) = 2;
    void withContentAdLoadedListener(IOnContentAdLoadedListener listener) = 3;
    void forCustomFormatAd(String customFormatId, IOnCustomTemplateAdLoadedListener onCustomFormatAdLoadedListener, IOnCustomClickListener onCustomClickListener) = 4;
    void withNativeAdOptions(in NativeAdOptionsParcel options) = 5;
    void withCorrelationIdProvider(ICorrelationIdProvider provider) = 6;
    void forAdView(IOnPublisherAdViewLoadedListener onPublisherAdViewLoadedListener, in AdSizeParcel adSize) = 7;
    void withPublisherAdViewOptions(in PublisherAdViewOptions options) = 8;
    void forNativeAd(IOnUnifiedNativeAdLoadedListener listener) = 9;
    void forInstreamAd(in InstreamAdConfigurationParcel config) = 12;
    void setInstreamAdLoadCallback(IInstreamAdLoadCallback callback) = 13;
    void withAdManagerAdViewOptions(in AdManagerAdViewOptions options) = 14;
}
