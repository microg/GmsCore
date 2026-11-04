/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import com.google.android.gms.common.api.Status;

interface ISharingCallback {
    void onResult(in Status status);
}
