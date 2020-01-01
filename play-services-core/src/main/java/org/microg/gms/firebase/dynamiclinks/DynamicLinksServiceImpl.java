/*
 * Copyright (C) 2019 e Foundation
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

package org.microg.gms.firebase.dynamiclinks;

import android.os.Parcel;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.CommonStatusCodes;

import com.google.firebase.dynamiclinks.internal.IDynamicLinksService;
import com.google.firebase.dynamiclinks.internal.IDynamicLinksCallbacks;
import com.google.firebase.dynamiclinks.internal.DynamicLinkData;
import com.google.firebase.dynamiclinks.internal.ShortDynamicLink;


public class DynamicLinksServiceImpl extends IDynamicLinksService.Stub {
    private static final String TAG = "GmsFrbDynamicLinksServImpl";

    public DynamicLinksServiceImpl(Context context, String packageName, Bundle extras) {
    }


    @Override
    public void getInitialLink(IDynamicLinksCallbacks callback, String var2) throws RemoteException {
        callback.onStatusDynamicLinkData(Status.SUCCESS, new DynamicLinkData());
    }


    @Override
    public void func2(IDynamicLinksCallbacks callback, Bundle var2) throws RemoteException {
        Log.d(TAG, "func2: " + callback + ", " + var2);
        callback.onStatusShortDynamicLink(Status.SUCCESS, new ShortDynamicLink());
    }


    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) {
            return true;
        }

        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
