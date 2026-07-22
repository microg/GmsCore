/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import androidx.annotation.Nullable;

public class WakeUpService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(new Handler()).getBinder();
    }
}
