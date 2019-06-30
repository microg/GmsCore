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

import android.os.RemoteException;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

public class WearableService extends BaseService {

    private WearableImpl wearable;

    public WearableService() {
        super("GmsWearSvc", GmsService.WEARABLE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ConfigurationDatabaseHelper configurationDatabaseHelper = new ConfigurationDatabaseHelper(getApplicationContext());
        NodeDatabaseHelper nodeDatabaseHelper = new NodeDatabaseHelper(getApplicationContext());
        wearable = new WearableImpl(getApplicationContext(), nodeDatabaseHelper, configurationDatabaseHelper);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wearable.stop();
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        callback.onPostInitComplete(0, new WearableServiceImpl(this, wearable, request.packageName), null);
    }
}
