/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

interface IVideoLifecycleCallbacks {
    void onVideoStart() = 0;
    void onVideoPlay() = 1;
    void onVideoPause() = 2;
    void onVideoEnd() = 3;
    void onVideoMute(boolean muted) = 4;
}
