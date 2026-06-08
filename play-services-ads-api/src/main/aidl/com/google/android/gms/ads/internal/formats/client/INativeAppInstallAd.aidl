/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.formats.client;

import android.os.Bundle;
import com.google.android.gms.ads.internal.client.IVideoController;
import com.google.android.gms.ads.internal.formats.client.INativeAdImage;
import com.google.android.gms.dynamic.IObjectWrapper;

interface INativeAppInstallAd {
    String getHeadline() = 0;
    List getImages() = 1;
    String getBody() = 2;
    INativeAdImage getIcon() = 3;
    String getCallToAction() = 4;
    double getStarRating() = 5;
    String getStore() = 6;
    String getPrice() = 7;
    Bundle getExtras() = 8;
    void recordImpression() = 9;
    void performClick(in Bundle clickData) = 10;
    void reportTouchEvent(in Bundle touchData) = 11;
    IVideoController getVideoController() = 12;
    IObjectWrapper getMediaContent() = 13;
}
