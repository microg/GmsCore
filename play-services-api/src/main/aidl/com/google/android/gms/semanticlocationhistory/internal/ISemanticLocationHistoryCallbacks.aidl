/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.semanticlocationhistory.InferredPlace;
import com.google.android.gms.semanticlocationhistory.UserLocationProfile;
import com.google.android.gms.semanticlocationhistory.ExperimentVisitsResponse;
import com.google.android.gms.semanticlocationhistory.LocationHistorySettings;
import com.google.android.gms.common.api.ApiMetadata;

interface ISemanticLocationHistoryCallbacks {
    void onSegmentsReturn(in DataHolder dataHolder) = 4;
    void onDeleteHistoryReturn(in Status status) = 5;
    void onEditSegmentsReturn(in Status status) = 3;
    void onGetBackupSummaryReturn(in Status status, in List list) = 7;
    void onGetInferredHomeReturn(in Status status, in InferredPlace inferredPlace) = 1;
    void onGetInferredWorkReturn(in Status status, in InferredPlace inferredPlace) = 2;
    void onGetUserLocationProfileReturn(in Status status, in UserLocationProfile userLocationProfile) = 6;
    void onExperimentVisitsReturn(in Status status, in ExperimentVisitsResponse experimentVisitsResponse, in ApiMetadata apiMetadata) = 8;
    void onLocationHistorySettingsReturn(in Status status, in LocationHistorySettings locationHistorySettings, in ApiMetadata apiMetadata) = 9;
}