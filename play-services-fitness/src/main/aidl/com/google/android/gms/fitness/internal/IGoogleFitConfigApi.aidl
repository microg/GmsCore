/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.request.DataTypeCreateRequest;
import com.google.android.gms.fitness.request.DisableFitRequest;
import com.google.android.gms.fitness.request.ReadDataTypeRequest;

interface IGoogleFitConfigApi {
    void createCustomDataType(in DataTypeCreateRequest request) = 0;
    void readDataType(in ReadDataTypeRequest request) = 1;
    void disableFit(in DisableFitRequest request) = 21;
}
