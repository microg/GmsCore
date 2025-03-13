/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.request.SessionStartRequest;
import com.google.android.gms.fitness.request.SessionStopRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.request.SessionRegistrationRequest;
import com.google.android.gms.fitness.request.SessionUnregistrationRequest;

interface IGoogleFitSessionsApi {
    void startRequest(in SessionStartRequest startRequest) = 0;
    void stopRequest(in SessionStopRequest stopRequest) = 1;
    void insertRequest(in SessionInsertRequest insetRequest) = 2;
    void readRequest(in SessionReadRequest readRequest) = 3;
    void registrationRequest(in SessionRegistrationRequest registrationRequest) = 4;
    void unRegistrationRequest(in SessionUnregistrationRequest unRegistrationRequest) = 5;
}