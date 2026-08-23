/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.rcs.RcsConfiguration;

interface IRcsCallbacks {
    void onRcsConfig(in Status status, in RcsConfiguration config);
    void onRcsStatusChanged(int statusCode);
}
