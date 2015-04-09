/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.icing;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.AbstractGmsServiceBroker;
import org.microg.gms.common.Services;

public class IndexService extends Service {
    private static final String TAG = "GmsIcingIndexSvc";

    private AppDataSearchImpl appDataSearch = new AppDataSearchImpl();
    private AbstractGmsServiceBroker broker = new AbstractGmsServiceBroker(
            Services.INDEX.SERVICE_ID, Services.SEARCH_ADMINISTRATION.SERVICE_ID,
            Services.SEARCH_CORPORA.SERVICE_ID, Services.SEARCH_GLOBAL.SERVICE_ID,
            Services.SEARCH_IME.SERVICE_ID, Services.SEARCH_QUERIES.SERVICE_ID) {
        @Override
        public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request) throws RemoteException {
            Log.d(TAG, "bound by: " + request);
            callback.onPostInitComplete(0, appDataSearch.asBinder(), null);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return broker.asBinder();
    }
}
