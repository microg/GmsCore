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

package org.microg.gms.wearable;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.Messenger;
import android.os.RemoteException;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

public class WearableService extends BaseService {

    private static WearableImpl wearable;

    public WearableService() {
        super("GmsWearSvc", GmsService.WEARABLE);
    }

    private synchronized static WearableImpl getWearable(Context appCtx) {
        if (wearable == null) {
            ConfigurationDatabaseHelper configurationDatabaseHelper = new ConfigurationDatabaseHelper(appCtx);
            NodeDatabaseHelper nodeDatabaseHelper = new NodeDatabaseHelper(appCtx);
            wearable = new WearableImpl(appCtx, nodeDatabaseHelper, configurationDatabaseHelper);
        }
        return wearable;
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        PackageUtils.checkPackageUid(this, request.packageName, Binder.getCallingUid());
        callback.onPostInitComplete(0, new WearableServiceImpl(this, getWearable(getApplicationContext()), request.packageName), null);
    }
}
