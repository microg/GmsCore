/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import org.microg.gms.cast.CastDynamiteModule;

public class ReconnectionService extends Service {
    private IReconnectionService delegate;

    @Override
    public void onCreate() {
        CastContext castContext = CastContext.getSharedInstance(this);
        delegate = CastDynamiteModule.newReconnectionService(this, castContext.getSessionManager().getWrappedThis(), castContext.getDiscoveryManager().getWrappedThis());
        if (delegate != null) {
            try {
                delegate.onCreate();
            } catch (RemoteException e) {
                // Ignore
            }
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (delegate != null) {
            try {
                delegate.onStartCommand(intent, flags, startId);
            } catch (RemoteException e) {
                // Ignore
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (delegate != null) {
            try {
                return delegate.onBind(intent);
            } catch (RemoteException e) {
                // Ignore
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        if (delegate != null) {
            try {
                delegate.onDestroy();
            } catch (RemoteException e) {
                // Ignore
            }
        }
        super.onDestroy();
    }
}
