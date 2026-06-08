/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.instream.client;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IInstreamAd {
    void destroy() = 0;
    IObjectWrapper getMediaContent() = 1;
    float getAspectRatio() = 2;
}
