/*
 * SPDX-FileCopyrightText: 2023, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import android.content.Intent;

import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

@PublicApi
public interface SharingClient {
    int VISIBILITY_HIDDEN = 0;
    int VISIBILITY_CONTACTS_ONLY = 1;
    int VISIBILITY_EVERYONE = 2;

    int SURFACE_BACKGROUND = 0;
    int SURFACE_FOREGROUND = 1;

    Task<Boolean> isSharingEnabled();

    Task<Void> setSharingEnabled(boolean enabled);

    Task<Integer> getDeviceVisibility();

    Task<Void> setDeviceVisibility(int visibility);

    Task<Void> registerReceiveSurface(ShareCallback callback, int surfaceType);

    Task<Void> unregisterReceiveSurface(ShareCallback callback);

    Task<Void> registerSendSurface(ShareCallback transferCallback, ShareCallback discoveryCallback, int surfaceType);

    Task<Void> unregisterSendSurface(ShareCallback transferCallback);

    Task<Void> send(ShareTarget shareTarget, Intent contentIntent);

    Task<Void> accept(ShareTarget shareTarget);

    Task<Void> reject(ShareTarget shareTarget);

    Task<Void> cancel(ShareTarget shareTarget);
}
