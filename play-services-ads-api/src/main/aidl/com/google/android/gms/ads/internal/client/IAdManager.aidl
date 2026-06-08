/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdRequestParcel;
import com.google.android.gms.ads.internal.client.AdSizeParcel;
import com.google.android.gms.ads.internal.client.IAdListener;
import com.google.android.gms.ads.internal.client.IAppEventListener;
import com.google.android.gms.ads.internal.client.IAdClickListener;
import com.google.android.gms.ads.internal.client.IVideoController;
import com.google.android.gms.ads.internal.client.IOnPaidEventListener;
import com.google.android.gms.ads.internal.client.IAdLoadCallback;
import com.google.android.gms.ads.internal.client.IFullScreenContentCallback;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IAdManager {
    IObjectWrapper getView() = 0;
    void destroy() = 1;
    boolean loadAd(in AdRequestParcel request) = 3;
    void pause() = 4;
    void resume() = 5;
    void setAdListener(IAdListener listener) = 6;
    void setAppEventListener(IAppEventListener listener) = 7;
    AdSizeParcel getAdSize() = 11;
    void setAdSize(in AdSizeParcel adSize) = 12;
    void setAdClickListener(IAdClickListener listener) = 19;
    void setManualImpressionFlag(int flag) = 21;
    IVideoController getVideoController() = 25;
    void setManualImpressionsEnabled(boolean enabled) = 33;
    void setOnPaidEventListener(IOnPaidEventListener listener) = 41;
    void loadAdWithCallback(in AdRequestParcel request, IAdLoadCallback callback) = 42;
    void showInterstitial(IObjectWrapper activityWrapper) = 43;
    void setFullScreenContentCallback(IFullScreenContentCallback callback) = 44;
    void setStartTimestampMillis(long timestampMillis) = 47;
}
