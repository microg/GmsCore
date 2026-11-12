/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework.internal;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.cast.framework.IReconnectionService;

/**
 * Module side of {@code ReconnectionService}, which the client starts to keep its process alive while a session is
 * active. A session of a process that was killed is picked up again by {@link SessionManagerImpl} once the client
 * creates its CastContext, so there is nothing to do here.
 */
public class ReconnectionServiceImpl extends IReconnectionService.Stub {
    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
    }
}
