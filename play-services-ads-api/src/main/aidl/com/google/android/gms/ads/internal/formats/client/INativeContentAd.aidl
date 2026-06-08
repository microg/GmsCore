/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.formats.client;

import android.os.Bundle;
import com.google.android.gms.ads.internal.client.IVideoController;
import com.google.android.gms.ads.internal.formats.client.INativeAdImage;
import com.google.android.gms.dynamic.IObjectWrapper;

interface INativeContentAd {
    String getHeadline() = 0;
    List getImages() = 1;
    String getBody() = 2;
    String getCallToAction() = 3;
    String getAdvertiser() = 4;
    INativeAdImage getLogo() = 5;
    Bundle getExtras() = 6;
    void recordImpression() = 7;
    void performClick(in Bundle clickData) = 8;
    void reportTouchEvent(in Bundle touchData) = 9;
    IVideoController getVideoController() = 10;
    IObjectWrapper getMediaContent() = 11;
}
