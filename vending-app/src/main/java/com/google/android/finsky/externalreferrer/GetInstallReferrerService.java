/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.externalreferrer;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class GetInstallReferrerService extends Service {
    private static final String TAG = "FakeReferrerService";

    private final IGetInstallReferrerService.Stub service = new IGetInstallReferrerService.Stub() {
        // https://developer.android.com/google/play/installreferrer/igetinstallreferrerservice
        @Override
        public Bundle getInstallReferrer(Bundle request) throws RemoteException {
            Bundle result = new Bundle();
            result.putString("install_referrer", "utm_source=google-play&utm_medium=organic");
            result.putLong("referrer_click_timestamp_seconds", 0);
            result.putLong("referrer_click_timestamp_server_seconds", 0);
            result.putLong("install_begin_timestamp_seconds", 0);
            result.putLong("install_begin_timestamp_server_seconds", 0);
            result.putString("install_version", null);
            result.putBoolean("google_play_instant", false);
            return result;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return service.asBinder();
    }
}
