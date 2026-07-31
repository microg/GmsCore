/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.asterism;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.asterism.GetAsterismConsentRequest;
import com.google.android.gms.asterism.internal.IAsterismApiService;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.IStatusCallback;

public class AsterismServiceImpl extends IAsterismApiService.Stub {
    private static final String TAG = "GmsAsterismSvc";
    private static final String PREF_NAME = "asterism_consent";
    private static final String PERMISSION_RCS = "com.google.android.gms.permission.RCS";

    private final Context context;

    public AsterismServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public void getAsterismConsent(IStatusCallback callback, GetAsterismConsentRequest request) throws RemoteException {
        if (callback == null) {
            Log.w(TAG, "getAsterismConsent: callback is null");
            return;
        }
        try {
            if (context.checkCallingPermission(PERMISSION_RCS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Missing " + PERMISSION_RCS);
                try {
                    callback.onResult(Status.INTERNAL_ERROR);
                } catch (RemoteException re) {
                    Log.w(TAG, "Failed to deliver permission error callback", re);
                }
                return;
            }

            String account = (request != null && request.accountName != null) ? request.accountName : "unknown";
            Log.d(TAG, "getAsterismConsent for: " + redact(account));

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean consented = prefs.getBoolean(account, true);
            try {
                callback.onResult(consented ? Status.SUCCESS : Status.CANCELED);
            } catch (RemoteException re) {
                Log.w(TAG, "Failed to deliver consent result", re);
            }
        } catch (Exception e) {
            Log.w(TAG, "getAsterismConsent failed defensively; replying SUCCESS to avoid caller deadlock", e);
            try {
                callback.onResult(Status.SUCCESS);
            } catch (RemoteException re) {
                Log.w(TAG, "Failed to deliver defensive SUCCESS callback", re);
            }
        }
    }

    private String redact(String account) {
        if (account == null || account.length() <= 4) return "***";
        return account.substring(0, 2) + "***" + account.substring(account.length() - 2);
    }
}
