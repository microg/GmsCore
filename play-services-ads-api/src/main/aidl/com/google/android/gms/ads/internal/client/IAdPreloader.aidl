/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import android.os.Bundle;
import com.google.android.gms.ads.internal.client.PreloadConfigurationParcel;
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator;

interface IAdPreloader {
    void startPreloading(in List<PreloadConfigurationParcel> configurations, IBinder callback) = 0;
    boolean isRewardedAdAvailable(String adUnitId) = 1;
    IBinder pollRewardedAd(String adUnitId) = 2;
    boolean isAppOpenAdAvailable(String adUnitId) = 3;
    IBinder pollAppOpenAd(String adUnitId) = 4;
    boolean isInterstitialAdAvailable(String adUnitId) = 5;
    IBinder pollInterstitialAd(String adUnitId) = 6;
    void setAdapterCreator(IAdapterCreator adapterCreator) = 7;
    boolean start(String preloadId, in PreloadConfigurationParcel configuration, IBinder callback) = 8;
    boolean isAdAvailable(int adFormat, String preloadId) = 9;
    IBinder pollInterstitialAdById(String preloadId) = 10;
    IBinder pollAppOpenAdById(String preloadId) = 11;
    IBinder pollRewardedAdById(String preloadId) = 12;
    PreloadConfigurationParcel getConfiguration(int adFormat, String preloadId) = 13;
    Bundle getConfigurations(int adFormat) = 14;
    int getNumAdsAvailable(int adFormat, String preloadId) = 15;
    boolean destroy(int adFormat, String preloadId) = 16;
    void destroyAll(int adFormat) = 17;
}
