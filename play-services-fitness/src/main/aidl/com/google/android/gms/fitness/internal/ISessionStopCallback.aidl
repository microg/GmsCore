/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.result.SessionStopResult;

interface ISessionStopCallback {
    void onResult(in SessionStopResult sessionStopReult) = 0;
}