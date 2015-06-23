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

import android.os.RemoteException;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.Services;

public class IndexService extends BaseService {
    private AppDataSearchImpl appDataSearch = new AppDataSearchImpl();
    private GlobalSearchAdminImpl globalSearchAdmin = new GlobalSearchAdminImpl();
    private SearchCorporaImpl searchCorpora = new SearchCorporaImpl();
    private SearchQueriesImpl searchQueries = new SearchQueriesImpl();

    public IndexService() {
        super("GmsIcingIndexSvc",
                Services.INDEX.SERVICE_ID, Services.SEARCH_ADMINISTRATION.SERVICE_ID,
                Services.SEARCH_CORPORA.SERVICE_ID, Services.SEARCH_GLOBAL.SERVICE_ID,
                Services.SEARCH_IME.SERVICE_ID, Services.SEARCH_QUERIES.SERVICE_ID);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request) throws RemoteException {
        switch (request.serviceId) {
            case Services.INDEX.SERVICE_ID:
                callback.onPostInitComplete(0, appDataSearch.asBinder(), null);
                break;
            case Services.SEARCH_ADMINISTRATION.SERVICE_ID:
                callback.onPostInitComplete(CommonStatusCodes.ERROR, null, null);
                break;
            case Services.SEARCH_QUERIES.SERVICE_ID:
                callback.onPostInitComplete(0, searchQueries.asBinder(), null);
                break;
            case Services.SEARCH_GLOBAL.SERVICE_ID:
                callback.onPostInitComplete(0, globalSearchAdmin.asBinder(), null);
                break;
            case Services.SEARCH_CORPORA.SERVICE_ID:
                callback.onPostInitComplete(0, searchCorpora.asBinder(), null);
                break;
            case Services.SEARCH_IME.SERVICE_ID:
                callback.onPostInitComplete(CommonStatusCodes.ERROR, null, null);
                break;
        }
    }
}
