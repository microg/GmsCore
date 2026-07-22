/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.result.SessionReadResult;

interface ISessionReadCallback {
    void onResult(in SessionReadResult sessionReadResult) = 0;
}