/*
 * Copyright 2013-2015 microG Project Team
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
import android.os.RemoteException;

import com.google.android.gms.location.places.PlaceReport;
import com.google.android.gms.location.reporting.ReportingState;
import com.google.android.gms.location.reporting.UploadRequest;
import com.google.android.gms.location.reporting.UploadRequestResult;
import com.google.android.gms.location.reporting.internal.IReportingService;

public class ReportingServiceImpl extends IReportingService.Stub {
    @Override
    public ReportingState getReportingState(Account account) throws RemoteException {
        return new ReportingState();
    }

    @Override
    public int tryOptIn(Account account) throws RemoteException {
        return 0;
    }

    @Override
    public UploadRequestResult requestUpload(UploadRequest request) throws RemoteException {
        return new UploadRequestResult();
    }

    @Override
    public int cancelUploadRequest(long l) throws RemoteException {
        return 0;
    }

    @Override
    public int reportDeviceAtPlace(Account account, PlaceReport report) throws RemoteException {
        return 0;
    }
}
