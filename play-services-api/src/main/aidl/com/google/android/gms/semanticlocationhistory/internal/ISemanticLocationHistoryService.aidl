/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.semanticlocation.SemanticLocationState;
import com.google.android.gms.semanticlocationhistory.LocationHistorySegment;
import com.google.android.gms.semanticlocationhistory.LocationHistorySegmentRequest;
import com.google.android.gms.semanticlocationhistory.RequestCredentials;
import com.google.android.gms.semanticlocationhistory.SemanticLocationEditInputs;
import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryCallbacks;

interface ISemanticLocationHistoryService {
    void getSegments(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in LocationHistorySegmentRequest request, in ApiMetadata apiMetadata) = 0;
    void onDemandBackup(in IStatusCallback callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 1;
    void onDemandRestore(in IStatusCallback callback, in RequestCredentials requestCredentials, in List/*<Long>*/ list, in ApiMetadata apiMetadata) = 2;
    void getInferredHome(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 3;
    void getInferredWork(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 4;
    void editSegments(in ISemanticLocationHistoryCallbacks callback, in List<LocationHistorySegment> list, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 5;
    void deleteHistory(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, long startTime, long endTime, in ApiMetadata apiMetadata) = 6;
    void getUserLocationProfile(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 7;
    void getBackupSummary(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 8;
    void deleteBackups(in IStatusCallback callback, in RequestCredentials requestCredentials, in List list, in ApiMetadata apiMetadata) = 9;
    void getLocationHistorySettings(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 10;
    void getExperimentVisits(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 11;
    void editCsl(in IStatusCallback callback, in RequestCredentials requestCredentials, in SemanticLocationEditInputs editInputs, in SemanticLocationState state, in ApiMetadata apiMetadata) = 12;
}