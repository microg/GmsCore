/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.licensing;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class LicensingService extends Service {
    private static final String TAG = "FakeLicenseService";

    private final ILicensingService.Stub mLicenseService = new ILicensingService.Stub() {

        @Override
        public void checkLicense(long nonce, String packageName, ILicenseResultListener listener) throws RemoteException {
            Log.d(TAG, "checkLicense(" + nonce + ", " + packageName + ")");
            // We don't return anything yet. Seems to work good for some checkers.
        }

        @Override
        public void checkLicenseV2(String packageName, ILicenseV2ResultListener listener, Bundle extraParams) throws RemoteException {
            Log.d(TAG, "checkLicenseV2(" + packageName + ", " + extraParams + ")");
            // We don't return anything yet. Seems to work good for some checkers.
        }
    };

    public IBinder onBind(Intent intent) {
        return mLicenseService;
    }
}
