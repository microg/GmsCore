/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.formats.client;

import com.google.android.gms.ads.internal.client.IVideoController;
import com.google.android.gms.ads.internal.formats.client.INativeAdImage;
import com.google.android.gms.dynamic.IObjectWrapper;

interface INativeCustomTemplateAd {
    List<String> getAvailableAssetNames() = 0;
    String getCustomTemplateId() = 1;
    String getText(String assetName) = 2;
    INativeAdImage getImage(String assetName) = 3;
    void performClick(String assetName) = 4;
    void recordImpression() = 5;
    IVideoController getVideoController() = 6;
    IObjectWrapper getMediaContent() = 7;
    IObjectWrapper getDisplayOpenMeasurement() = 8;
}
