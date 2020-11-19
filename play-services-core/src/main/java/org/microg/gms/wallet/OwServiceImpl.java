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

package org.microg.gms.wallet;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.internal.IOwService;
import com.google.android.gms.wallet.internal.IWalletServiceCallbacks;

public class OwServiceImpl extends IOwService.Stub {
    private static final String TAG = "GmsWalletOwSvc";
    private Context context;

    public OwServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public void isReadyToPay(IsReadyToPayRequest request, Bundle args, IWalletServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "isReadyToPay: " + request.toJson());
        try {
            callbacks.onIsReadyToPayResponse(Status.SUCCESS, false, Bundle.EMPTY);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
