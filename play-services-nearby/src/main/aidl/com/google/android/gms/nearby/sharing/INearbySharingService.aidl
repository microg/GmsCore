/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import android.os.IBinder;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.sharing.ISharingCallback;

interface INearbySharingService {
    void registerSharingProvider(ISharingCallback callback);
    void unregisterSharingProvider(ISharingCallback callback);
    void registerSendSurface(IBinder token, ISharingCallback callback, int mode);
    void unregisterSendSurface(IBinder token, ISharingCallback callback);
    void registerReceiveSurface(IBinder token, ISharingCallback callback, int mode);
    void unregisterReceiveSurface(IBinder token, ISharingCallback callback);
    void setAccount(String accountName, IStatusCallback callback);
    void setDeviceName(String deviceName, IStatusCallback callback);
    void setVisibility(int visibility, IStatusCallback callback);
    String getDeviceName();
    int getVisibility();
}
