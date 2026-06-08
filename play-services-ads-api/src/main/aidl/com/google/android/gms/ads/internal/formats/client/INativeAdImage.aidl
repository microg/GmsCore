/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.formats.client;

import android.net.Uri;
import com.google.android.gms.dynamic.IObjectWrapper;

interface INativeAdImage {
    IObjectWrapper getDrawable() = 0;
    Uri getUri() = 1;
    double getScale() = 2;
    int getWidth() = 3;
    int getHeight() = 4;
}
