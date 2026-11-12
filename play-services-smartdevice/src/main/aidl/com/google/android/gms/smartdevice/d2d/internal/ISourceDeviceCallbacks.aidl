/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.smartdevice.d2d.BootstrapAccount;

oneway interface ISourceDeviceCallbacks {
    void onScanStartResult(in Status status) = 0;
    void onScanStopResult(in Status status) = 1;
    void onConnectResult(in Status status) = 2;
    void onBootstrapResult(in Status status) = 3;
    void onContinueWithPinResult(in Status status) = 4;
    void onDisconnectResult(in Status status) = 5;
    void onResumeBootstrapResult(in Status status) = 6;
    void onUpdateConfigurationsResult(in Status status) = 7;
    void onAccountTransferResults(in Status status, in List<BootstrapAccount> results) = 8;
    void onStartDirectTransferResult(in Status status) = 9;
    void onAbortDirectTransferResult(in Status status) = 10;
    void onIsTransferInProgressResult(in Status status) = 11;
}
