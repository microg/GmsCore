/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.Bundle;
import com.google.android.gms.common.internal.ConnectionInfo;

interface IGmsCallbacks {
    void onPostInitComplete(int statusCode, IBinder binder, in Bundle params);
    void onAccountValidationComplete(int statusCode, in Bundle params);
    void onPostInitCompleteWithConnectionInfo(int statusCode, IBinder binder, in ConnectionInfo info);
}
