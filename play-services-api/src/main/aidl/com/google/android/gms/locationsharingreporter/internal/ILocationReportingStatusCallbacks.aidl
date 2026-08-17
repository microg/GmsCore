/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.locationsharingreporter.LocationReportingStatus;

interface ILocationReportingStatusCallbacks {
    void onLocationReportingStatus(in Status status, in LocationReportingStatus locationReportingStatus, in ApiMetadata apiMetadata) = 0;
}