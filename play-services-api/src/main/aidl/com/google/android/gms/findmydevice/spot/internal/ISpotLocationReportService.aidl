/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot.internal;

import com.google.android.gms.findmydevice.spot.internal.ISpotLocationReportCallbacks;
import com.google.android.gms.findmydevice.spot.LocationReportRequest;
import com.google.android.gms.findmydevice.spot.GetLocationReportingStateRequest;
import com.google.android.gms.findmydevice.spot.DisableLocationReportingRequest;

interface ISpotLocationReportService {
    void locationReport(ISpotLocationReportCallbacks callbacks, in LocationReportRequest request) = 2;
    void getLocationReportingState(ISpotLocationReportCallbacks callbacks, in GetLocationReportingStateRequest request) = 6;
    void disableLocationReporting(ISpotLocationReportCallbacks callbacks, in DisableLocationReportingRequest request) = 7;
}