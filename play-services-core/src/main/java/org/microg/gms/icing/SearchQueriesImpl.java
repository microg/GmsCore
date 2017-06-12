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

import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.search.queries.QueryRequest;
import com.google.android.gms.search.queries.QueryResponse;
import com.google.android.gms.search.queries.internal.ISearchQueriesCallbacks;
import com.google.android.gms.search.queries.internal.ISearchQueriesService;

public class SearchQueriesImpl extends ISearchQueriesService.Stub {
    private static final String TAG = "GmsIcingQueriesImpl";

    @Override
    public void query(QueryRequest request, ISearchQueriesCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "query: " + request);
        callbacks.onQuery(new QueryResponse(Status.CANCELED, null));
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
