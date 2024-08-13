/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.os.IBinder;

public interface DataBinder<T> {
    T getBinderData(IBinder iBinder) throws Exception;
}
