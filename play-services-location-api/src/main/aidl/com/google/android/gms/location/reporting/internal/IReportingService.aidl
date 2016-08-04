package com.google.android.gms.location.reporting.internal;

import android.accounts.Account;
import com.google.android.gms.location.places.PlaceReport;
import com.google.android.gms.location.reporting.ReportingState;
import com.google.android.gms.location.reporting.UploadRequest;
import com.google.android.gms.location.reporting.UploadRequestResult;

interface IReportingService {
    ReportingState getReportingState(in Account account) = 0;
    int tryOptIn(in Account account) = 1;
    UploadRequestResult requestUpload(in UploadRequest request) = 2;
    int cancelUploadRequest(long l) = 3;
    int reportDeviceAtPlace(in Account account, in PlaceReport report) = 4;
}
