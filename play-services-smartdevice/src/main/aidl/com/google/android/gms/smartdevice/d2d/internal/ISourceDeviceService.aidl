/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.internal;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.smartdevice.d2d.BootstrapConfigurations;
import com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener;
import com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener2;
import com.google.android.gms.smartdevice.d2d.internal.ISourceDeviceCallbacks;

oneway interface ISourceDeviceService {
    // 0-7 are reserved.
    void getBootstrappableAccounts(ISourceDeviceCallbacks callbacks, in ApiMetadata apiMetadata) = 8;
    void startDirectTransfer(ISourceDeviceCallbacks callbacks, in BootstrapConfigurations configuration, in ParcelFileDescriptor[] pipes, IDirectTransferListener transferListener, in ApiMetadata apiMetadata) = 9;
    void abortDirectTransfer(ISourceDeviceCallbacks callbacks, in ApiMetadata apiMetadata) = 10;

    // 11-14 are reserved.
    void isTransferInProgress(ISourceDeviceCallbacks callbacks, in Bundle extras, in ApiMetadata apiMetadata) = 15;

    // 16-21 are reserved.
    void startDirectTransfer2(ISourceDeviceCallbacks callbacks, in BootstrapConfigurations configuration, in ParcelFileDescriptor[] pipes, IDirectTransferListener2 transferListener, in ApiMetadata apiMetadata) = 22;
}
