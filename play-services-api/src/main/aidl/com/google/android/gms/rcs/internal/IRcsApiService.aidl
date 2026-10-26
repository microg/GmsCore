/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs.internal;

import com.google.android.gms.rcs.RcsConfigRequest;
import com.google.android.gms.rcs.internal.IRcsCallbacks;

interface IRcsApiService {
    void getRcsConfiguration(in RcsConfigRequest request, IRcsCallbacks callbacks);
    void registerRcsListener(IRcsCallbacks callbacks);
    void unregisterRcsListener(IRcsCallbacks callbacks);
}
