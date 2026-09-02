/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.client.IVideoLifecycleCallbacks;

interface IVideoController {
    float getAspectRatio() = 0;
    float getCurrentTime() = 1;
    float getDuration() = 2;
    int getPlaybackState() = 3;
    void play() = 4;
    void pause() = 5;
    void stop() = 6;
    IVideoLifecycleCallbacks getVideoLifecycleCallbacks() = 7;
    void setVideoLifecycleCallbacks(IVideoLifecycleCallbacks callbacks) = 8;
    boolean isClickToExpandEnabled() = 9;
    boolean isCustomControlsEnabled() = 10;
    boolean isMuted() = 11;
    void mute(boolean mute) = 12;
}
