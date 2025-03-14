/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.internal;

import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryCallbacks;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.semanticlocationhistory.RequestCredentials;
import com.google.android.gms.semanticlocationhistory.LocationHistorySegmentRequest;
import com.google.android.gms.common.api.internal.IStatusCallback;

interface ISemanticLocationHistoryService {
    void getSegments(in ISemanticLocationHistoryCallbacks callback, in ApiMetadata apiMetadata, in RequestCredentials requestCredentials, in LocationHistorySegmentRequest request) = 0;
    void onDemandBackupRestore(in IStatusCallback callback, in ApiMetadata apiMetadata, in RequestCredentials requestCredentials) = 1;
    void onDemandBackupRestoreV2(in IStatusCallback callback, in RequestCredentials requestCredentials, in List list, in ApiMetadata apiMetadata) = 2;
    void getInferredHome(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 3;
    void getInferredWork(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, in ApiMetadata apiMetadata) = 4;
    void editSegments(in ISemanticLocationHistoryCallbacks callback, in List list, in ApiMetadata apiMetadata, in RequestCredentials requestCredentials) = 5;
    void deleteHistory(in ISemanticLocationHistoryCallbacks callback, in RequestCredentials requestCredentials, long startTime, long endTime, in ApiMetadata apiMetadata) = 6;
    void getUserLocationProfile(in IStatusCallback callback, in ApiMetadata apiMetadata, in RequestCredentials requestCredentials) = 7;
    void getBackupSummary(in IStatusCallback callback, in ApiMetadata apiMetadata, in RequestCredentials requestCredentials) = 8;
    void deleteBackups(in IStatusCallback callback, in RequestCredentials requestCredentials, in List list, in ApiMetadata apiMetadata) = 9;
}