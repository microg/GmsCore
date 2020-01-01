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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.RemoteException;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

import org.microg.gms.appinvite.AppInviteServiceImpl;

public class AppInviteService extends BaseService {
    private static final String TAG = "GmsAppInviteService";

    public AppInviteService() {
        super("GmsAppInviteSvc", GmsService.APP_INVITE);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        Log.d(TAG, "callb: " + callback + " ; req: " + request + " ; serv: " + service);

        callback.onPostInitComplete(0, new AppInviteServiceImpl(this, request.packageName, request.extras), null);
    }
}
