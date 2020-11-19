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

package org.microg.gms.icing;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class IndexService extends BaseService {
    private AppDataSearchImpl appDataSearch = new AppDataSearchImpl();
    private GlobalSearchAdminImpl globalSearchAdmin = new GlobalSearchAdminImpl();
    private SearchCorporaImpl searchCorpora = new SearchCorporaImpl();
    private SearchQueriesImpl searchQueries = new SearchQueriesImpl();

    public IndexService() {
        super("GmsIcingIndexSvc",
                GmsService.INDEX, GmsService.SEARCH_ADMINISTRATION, GmsService.SEARCH_CORPORA,
                GmsService.SEARCH_GLOBAL, GmsService.SEARCH_IME, GmsService.SEARCH_QUERIES);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        switch (service) {
            case INDEX:
                callback.onPostInitComplete(0, appDataSearch.asBinder(), null);
                break;
            case SEARCH_ADMINISTRATION:
                Log.w(TAG, "Service not yet implemented: " + service);
                callback.onPostInitComplete(CommonStatusCodes.ERROR, null, null);
                break;
            case SEARCH_QUERIES:
                callback.onPostInitComplete(0, searchQueries.asBinder(), null);
                break;
            case SEARCH_GLOBAL:
                callback.onPostInitComplete(0, globalSearchAdmin.asBinder(), null);
                break;
            case SEARCH_CORPORA:
                callback.onPostInitComplete(0, searchCorpora.asBinder(), null);
                break;
            case SEARCH_IME:
                Log.w(TAG, "Service not yet implemented: " + service);
                callback.onPostInitComplete(CommonStatusCodes.ERROR, null, null);
                break;
        }
    }
}
