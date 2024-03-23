/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.GetSyncInfoRequest;
import com.google.android.gms.fitness.request.DataInsertRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.ReadStatsRequest;
import com.google.android.gms.fitness.request.ReadRawRequest;
import com.google.android.gms.fitness.request.DailyTotalRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.request.DataUpdateListenerRegistrationRequest;
import com.google.android.gms.fitness.request.DataUpdateListenerUnregistrationRequest;
import com.google.android.gms.fitness.request.GetFileUriRequest;
import com.google.android.gms.fitness.request.DebugInfoRequest;
import com.google.android.gms.fitness.request.DataPointChangesRequest;
import com.google.android.gms.fitness.request.SessionChangesRequest;

interface IGoogleFitHistoryApi {
    void readData(in DataReadRequest request) = 0;
    void insertData(in DataInsertRequest request) = 1;
    void deleteData(in DataDeleteRequest request) = 2;
    void getSyncInfo(in GetSyncInfoRequest request) = 3;
    void readStats(in ReadStatsRequest request) = 4;
    void readRaw(in ReadRawRequest request) = 5;
    void getDailyTotal(in DailyTotalRequest request) = 6;
    void insertDataPrivileged(in DataInsertRequest request) = 7;
    void updateData(in DataUpdateRequest request) = 8;
    void registerDataUpdateListener(in DataUpdateListenerRegistrationRequest request) = 9;
    void unregisterDataUpdateListener(in DataUpdateListenerUnregistrationRequest request) = 10;
    void getFileUri(in GetFileUriRequest request) = 11;
    void getDebugInfo(in DebugInfoRequest request) = 12;
    void getDataPointChanges(in DataPointChangesRequest request) = 15;
    void getSessionChanges(in SessionChangesRequest request) = 16;
}