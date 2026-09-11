/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.formats.client;

import android.os.Bundle;
import com.google.android.gms.ads.internal.client.IVideoController;
import com.google.android.gms.ads.internal.client.IResponseInfo;
import com.google.android.gms.ads.internal.formats.client.INativeAdImage;
import com.google.android.gms.ads.internal.formats.client.IAttributionInfo;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IUnifiedNativeAd {
    String getHeadline() = 0;
    List getImages() = 1;
    String getBody() = 2;
    INativeAdImage getIcon() = 3;
    String getCallToAction() = 4;
    double getStarRating() = 5;
    String getStore() = 6;
    String getPrice() = 7;
    String getAdvertiser() = 8;
    Bundle getExtras() = 9;
    IVideoController getVideoController() = 10;
    float getMediaContentAspectRatio() = 11;
    IResponseInfo getResponseInfo() = 12;
    IAttributionInfo getAttributionInfo() = 13;
    void recordImpression() = 14;
    void performClick(in Bundle clickData) = 15;
    void reportTouchEvent(in Bundle touchData) = 16;
    void cancelUnconfirmedClick() = 17;
    void recordCustomClickGesture() = 18;
    IObjectWrapper getMediaContent() = 19;
}
