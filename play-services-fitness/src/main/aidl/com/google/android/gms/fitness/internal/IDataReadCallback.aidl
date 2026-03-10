/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.request.DataReadResult;

interface IDataReadCallback {
    void onPostResult(in DataReadResult dataReadResult) = 0;
}