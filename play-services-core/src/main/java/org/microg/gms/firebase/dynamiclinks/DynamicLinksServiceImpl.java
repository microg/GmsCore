/*
 * SPDX-FileCopyrightText: 2019 e Foundation
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
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

    private String packageName;

    public DynamicLinksServiceImpl(Context context, String packageName, Bundle extras) {
        this.packageName = packageName;
    }


    @Override
    public void getDynamicLink(IDynamicLinksCallbacks callback, String link) throws RemoteException {
        if (link != null) {
            Uri linkUri = Uri.parse(link);
            String packageName = linkUri.getQueryParameter("apn");
            String amvParameter = linkUri.getQueryParameter("amv");
            if (packageName == null) {
                throw new RuntimeException("Missing package name");
            } else if (!this.packageName.equals(packageName)) {
                throw new RuntimeException("Registered package name:" + this.packageName + " does not match link package name: " + packageName);
            }
            int amv = 0;
            if (amvParameter != null && amvParameter != "") {
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
