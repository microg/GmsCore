/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework.media;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;

public class MediaNotificationService extends Service {
    private final IMediaNotificationService.Stub binder = new IMediaNotificationService.Stub() {
        @Override
        public void setNotificationActionsProvider(INotificationActionsProvider provider) throws RemoteException {
            // Implementation pending
        }

        @Override
        public void updateNotification(Notification notification) throws RemoteException {
            // Implementation pending
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
