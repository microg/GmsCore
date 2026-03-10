/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.pseudonymous.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.pseudonymous.PseudonymousIdToken;

interface IPseudonymousIdCallbacks {
    void onGetTokenResponse(in Status status, in PseudonymousIdToken token) = 0;
    void onSetTokenResponse(in Status status) = 1;
    void onGetLastResetWallTimeMsResponse(in Status status, long time) = 2;
}
