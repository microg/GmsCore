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

package org.microg.gms.location;

import android.accounts.Account;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.location.places.PlaceReport;
import com.google.android.gms.location.reporting.ReportingState;
import com.google.android.gms.location.reporting.UploadRequest;
import com.google.android.gms.location.reporting.UploadRequestResult;
import com.google.android.gms.location.reporting.internal.IReportingService;

public class ReportingServiceImpl extends IReportingService.Stub {
    private static final String TAG = "GmsLocReportSvcImpl";

    @Override
    public ReportingState getReportingState(Account account) throws RemoteException {
        Log.d(TAG, "getReportingState");
        return new ReportingState();
    }

    @Override
    public int tryOptIn(Account account) throws RemoteException {
        Log.d(TAG, "tryOptIn");
        return 0;
    }

    @Override
    public UploadRequestResult requestUpload(UploadRequest request) throws RemoteException {
        Log.d(TAG, "requestUpload");
        return new UploadRequestResult();
    }

    @Override
    public int cancelUploadRequest(long l) throws RemoteException {
        Log.d(TAG, "cancelUploadRequest");
        return 0;
    }

    @Override
    public int reportDeviceAtPlace(Account account, PlaceReport report) throws RemoteException {
        Log.d(TAG, "reportDeviceAtPlace");
        return 0;
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
