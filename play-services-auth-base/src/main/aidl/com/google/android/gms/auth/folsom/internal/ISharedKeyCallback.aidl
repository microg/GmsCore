/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.folsom.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.auth.folsom.SharedKey;

interface ISharedKeyCallback {
    void onResult(in Status status, in SharedKey[] sharedKeyArr);
}
