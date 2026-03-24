/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.findmydevice.spot.LocationReportResponse;
import com.google.android.gms.findmydevice.spot.OwnersLocationReportResponse;
import com.google.android.gms.findmydevice.spot.GetLocationReportingStateResponse;
import com.google.android.gms.findmydevice.spot.DisableLocationReportingResponse;

interface ISpotLocationReportCallbacks {
    void onLocationReport(in Status status, in LocationReportResponse response) = 2;
    void onOwnersLocationReport(in Status status, in OwnersLocationReportResponse response) = 4;
    void onGetLocationReportingState(in Status status, in GetLocationReportingStateResponse response) = 6;
    void onDisableLocationReporting(in Status status, in DisableLocationReportingResponse response) = 7;
}