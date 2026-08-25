/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.internal;

import android.accounts.Account;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.locationsharingreporter.internal.ILocationReportingIssuesCallback;
import com.google.android.gms.locationsharingreporter.internal.ILocationReportingStatusCallbacks;
import com.google.android.gms.locationsharingreporter.internal.ILocationUploadCallbacks;
import com.google.android.gms.locationsharingreporter.LocationUploadRequest;
import com.google.android.gms.locationsharingreporter.PeriodicLocationUploadRequest;
import com.google.android.gms.locationsharingreporter.StartLocationReportingRequest;
import com.google.android.gms.locationsharingreporter.StopLocationReportingRequest;
import com.google.android.gms.locationsharingreporter.NoticeAckedUpdateRequest;

interface ILocationSharingReporterService {
    void uploadLocation(ILocationUploadCallbacks callback, in Account account, in LocationUploadRequest request, in ApiMetadata apiMetadata) = 0;
    void getReportingStatus(ILocationReportingStatusCallbacks callback, in Account account, in ApiMetadata apiMetadata) = 1;
    void syncReportingStatus(IStatusCallback callback, in Account account, in ApiMetadata apiMetadata) = 2;
    void periodicLocationUpload(IStatusCallback callback, in Account account, in PeriodicLocationUploadRequest request, in ApiMetadata apiMetadata) = 3;
    void startLocationReporting(IStatusCallback callback, in Account account, in StartLocationReportingRequest request, in ApiMetadata apiMetadata) = 4;
    void stopLocationReporting(IStatusCallback callback, in Account account, in StopLocationReportingRequest request, in ApiMetadata apiMetadata) = 5;
    void updateNoticeState(IStatusCallback callback, in Account account, in NoticeAckedUpdateRequest request, in ApiMetadata apiMetadata) = 6;
    void getReportingIssues(ILocationReportingIssuesCallback callback, in Account account, in ApiMetadata apiMetadata) = 7;
}