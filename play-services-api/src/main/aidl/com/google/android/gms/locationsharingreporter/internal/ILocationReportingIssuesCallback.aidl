/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.locationsharingreporter.PeriodicLocationReportingIssues;

interface ILocationReportingIssuesCallback {
    void onResult(in Status status, in PeriodicLocationReportingIssues periodicLocationReportingIssues, in ApiMetadata apiMetadata) = 0;
}