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
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.search.global.GetCurrentExperimentIdsRequest;
import com.google.android.gms.search.global.GetCurrentExperimentIdsResponse;
import com.google.android.gms.search.global.GetGlobalSearchSourcesRequest;
import com.google.android.gms.search.global.GetGlobalSearchSourcesResponse;
import com.google.android.gms.search.global.GetPendingExperimentIdsRequest;
import com.google.android.gms.search.global.GetPendingExperimentIdsResponse;
import com.google.android.gms.search.global.SetExperimentIdsRequest;
import com.google.android.gms.search.global.SetExperimentIdsResponse;
import com.google.android.gms.search.global.SetIncludeInGlobalSearchRequest;
import com.google.android.gms.search.global.SetIncludeInGlobalSearchResponse;
import com.google.android.gms.search.global.internal.IGlobalSearchAdminCallbacks;
import com.google.android.gms.search.global.internal.IGlobalSearchAdminService;

public class GlobalSearchAdminImpl extends IGlobalSearchAdminService.Stub {
    private static final String TAG = "GmsIcingGlobalImpl";

    @Override
    public void getGlobalSearchSources(GetGlobalSearchSourcesRequest request, IGlobalSearchAdminCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getGlobalSearchSources: " + request);
        callbacks.onGetGlobalSearchSourcesResponse(new GetGlobalSearchSourcesResponse(Status.SUCCESS, new Parcelable[0]));
    }

    @Override
    public void setExperimentIds(SetExperimentIdsRequest request, IGlobalSearchAdminCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "setExperimentIds: " + request);
        callbacks.onSetExperimentIdsResponse(new SetExperimentIdsResponse(Status.SUCCESS));
    }

    @Override
    public void getCurrentExperimentIds(GetCurrentExperimentIdsRequest request, IGlobalSearchAdminCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getCurrentExperimentIds: " + request);
        callbacks.onGetCurrentExperimentIdsResponse(new GetCurrentExperimentIdsResponse(Status.SUCCESS, new int[0]));
    }

    @Override
    public void getPendingExperimentIds(GetPendingExperimentIdsRequest request, IGlobalSearchAdminCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getPendingExperimentIds: " + request);
        callbacks.onGetPendingExperimentIdsResponse(new GetPendingExperimentIdsResponse(Status.SUCCESS, new int[0]));
    }

    @Override
    public void setIncludeInGlobalSearch(SetIncludeInGlobalSearchRequest request, IGlobalSearchAdminCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "setIncludeInGlobalSearch: " + request);
        callbacks.onSetIncludeInGlobalSearchResponse(new SetIncludeInGlobalSearchResponse(Status.SUCCESS));
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
