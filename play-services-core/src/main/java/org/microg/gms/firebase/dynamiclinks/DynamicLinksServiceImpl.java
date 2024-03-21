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

import android.net.Uri;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;

import com.google.android.gms.common.api.Status;

import com.google.firebase.dynamiclinks.internal.IDynamicLinksService;
import com.google.firebase.dynamiclinks.internal.IDynamicLinksCallbacks;
import com.google.firebase.dynamiclinks.internal.DynamicLinkData;
import com.google.firebase.dynamiclinks.internal.ShortDynamicLinkImpl;


public class DynamicLinksServiceImpl extends IDynamicLinksService.Stub {
    private static final String TAG = "GmsDynamicLinksServImpl";

    private String mPackageName;

    public DynamicLinksServiceImpl(Context context, String packageName, Bundle extras) {
        mPackageName = packageName;
    }


    @Override
    public void getInitialLink(IDynamicLinksCallbacks callback, String link) throws RemoteException {
        if(link != null) {
            Uri linkUri = Uri.parse(link);
            String packageName = linkUri.getQueryParameter("apn");
            String amvParameter = linkUri.getQueryParameter("amv");
            if(packageName == null) {
                throw new RuntimeException("Missing package name");
            } else if (!mPackageName.equals(packageName)) {
                throw new RuntimeException("Registered package name:" + mPackageName + " does not match link package name: " + packageName);
            }
            int amv = 0;
            if(amvParameter != null && amvParameter != "") {
                amv = Integer.parseInt(amvParameter);
            }
            DynamicLinkData data = new DynamicLinkData(
                null,
                linkUri.getQueryParameter("link"),
                amv,
                0,
                null,
                null
            );
            callback.onStatusDynamicLinkData(Status.SUCCESS, data);
        } else {
            callback.onStatusDynamicLinkData(Status.SUCCESS, null);
        }
    }


    @Override
    public void createShortDynamicLink(IDynamicLinksCallbacks callback, Bundle extras) throws RemoteException {
        callback.onStatusShortDynamicLink(Status.SUCCESS, new ShortDynamicLinkImpl());
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
