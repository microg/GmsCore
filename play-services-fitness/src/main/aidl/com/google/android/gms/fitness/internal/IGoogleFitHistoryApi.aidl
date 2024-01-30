/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.GetSyncInfoRequest;
import com.google.android.gms.fitness.request.DataInsertRequest;
import com.google.android.gms.fitness.request.DataReadRequest;

interface IGoogleFitHistoryApi {
    void getDeleteData(in DataDeleteRequest dataDeleteRequest) = 0;
    void getSyncInfo(in GetSyncInfoRequest getSyncInfoRequest) = 1;
    void getInsertData(in DataInsertRequest dataInsertRequest) = 2;
    void getReadData(in DataReadRequest dataReadRequest) = 3;
}