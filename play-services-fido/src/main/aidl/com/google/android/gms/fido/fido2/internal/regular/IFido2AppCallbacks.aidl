/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.internal.regular;

import android.app.PendingIntent;
import com.google.android.gms.common.api.Status;

interface IFido2AppCallbacks {
    void onResult(in Status status, in PendingIntent pendingIntent);
}
