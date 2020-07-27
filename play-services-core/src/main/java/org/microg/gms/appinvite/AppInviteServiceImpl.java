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

package org.microg.gms.appinvite;

import android.os.Parcel;
import android.os.RemoteException;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.api.Status;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import com.google.android.gms.appinvite.internal.IAppInviteService;
import com.google.android.gms.appinvite.internal.IAppInviteCallbacks;


public class AppInviteServiceImpl extends IAppInviteService.Stub {
    private static final String TAG = "GmsAppInviteServImpl";

    public AppInviteServiceImpl(Context context, String packageName, Bundle extras) {
    }


    @Override
    public void updateInvitationOnInstall(IAppInviteCallbacks callback, String invitationId) throws RemoteException {
        callback.onStatus(Status.SUCCESS);
    }

    @Override
    public void convertInvitation(IAppInviteCallbacks callback, String invitationId) throws RemoteException {
        callback.onStatus(Status.SUCCESS);
    }

    @Override
    public void getInvitation(IAppInviteCallbacks callback) throws RemoteException {
        callback.onStatusIntent(new Status(Activity.RESULT_CANCELED), null);
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
