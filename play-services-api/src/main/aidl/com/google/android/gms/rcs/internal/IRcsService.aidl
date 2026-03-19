/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs.internal;

import com.google.android.gms.rcs.internal.IRcsCallbacks;

interface IRcsService {
    void getCapabilities(IRcsCallbacks callbacks) = 0;
    void isAvailable(IRcsCallbacks callbacks) = 1;
    void getConfiguration(IRcsCallbacks callbacks) = 2;
    void registerCapabilityCallback(IRcsCallbacks callbacks) = 3;
    void unregisterCapabilityCallback(IRcsCallbacks callbacks) = 4;
    void startProvisioning(IRcsCallbacks callbacks) = 5;
    void stopProvisioning(IRcsCallbacks callbacks) = 6;
    void getProvisioningStatus(IRcsCallbacks callbacks) = 7;
    void triggerReconfiguration(IRcsCallbacks callbacks) = 8;
}
