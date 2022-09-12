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
import android.content.Context;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.location.places.PlaceReport;
import com.google.android.gms.location.reporting.OptInRequest;
import com.google.android.gms.location.reporting.ReportingState;
import com.google.android.gms.location.reporting.SendDataRequest;
import com.google.android.gms.location.reporting.UlrPrivateModeRequest;
import com.google.android.gms.location.reporting.UploadRequest;
import com.google.android.gms.location.reporting.UploadRequestResult;
import com.google.android.gms.location.reporting.internal.IReportingService;

import org.microg.gms.common.PackageUtils;

public class ReportingServiceImpl extends IReportingService.Stub {
    private static final String TAG = "GmsLocReportSvcImpl";
    private Context context;

    public ReportingServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public ReportingState getReportingState(Account account) throws RemoteException {
        Log.d(TAG, "getReportingState");
        ReportingState state = new ReportingState();
        if (PackageUtils.callerHasExtendedAccess(context)) {
            state.deviceTag = 0;
        }
        return state;
    }

    @Override
    public int tryOptInAccount(Account account) throws RemoteException {
        OptInRequest request = new OptInRequest();
        request.account = account;
        return tryOptIn(request);
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
    public int tryOptIn(OptInRequest request) throws RemoteException {
        return 0;
    }

    @Override
    public int sendData(SendDataRequest request) throws RemoteException {
        return 0;
    }

    @Override
    public int requestPrivateMode(UlrPrivateModeRequest request) throws RemoteException {
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
