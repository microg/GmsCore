/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.semanticlocationhistory.ExperimentVisitsResponse;
import com.google.android.gms.semanticlocationhistory.InferredPlace;
import com.google.android.gms.semanticlocationhistory.LocationHistorySegment;
import com.google.android.gms.semanticlocationhistory.LocationHistorySettings;
import com.google.android.gms.semanticlocationhistory.OdlhBackupSummary;
import com.google.android.gms.semanticlocationhistory.UserLocationProfile;

interface ISemanticLocationHistoryCallbacks {
    void onSegmentListResponse(in Status status, in List<LocationHistorySegment> segments, in ApiMetadata apiMetadata) = 0;
    void onGetInferredHomeResponse(in Status status, in InferredPlace inferredPlace, in ApiMetadata apiMetadata) = 1;
    void onGetInferredWorkResponse(in Status status, in InferredPlace inferredPlace, in ApiMetadata apiMetadata) = 2;
    void onEditSegmentsResponse(in Status status, in ApiMetadata apiMetadata) = 3;
    void onGetSegmentsResponse(in DataHolder dataHolder, in ApiMetadata apiMetadata) = 4;
    void onDeleteHistoryResponse(in Status status, in ApiMetadata apiMetadata) = 5;
    void onGetUserLocationProfileResponse(in Status status, in UserLocationProfile userLocationProfile, in ApiMetadata apiMetadata) = 6;
    void onGetBackupSummaryResponse(in Status status, in List<OdlhBackupSummary> summaries, in ApiMetadata apiMetadata) = 7;
    void onLocationHistorySettings(in Status status, in LocationHistorySettings locationHistorySettings, in ApiMetadata apiMetadata) = 8;
    void onGetExperimentVisitsResponse(in Status status, in ExperimentVisitsResponse response, in ApiMetadata apiMetadata) = 9;
}