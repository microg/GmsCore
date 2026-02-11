/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot.internal;

import com.google.android.gms.findmydevice.spot.internal.ISpotManagementCallbacks;
import com.google.android.gms.findmydevice.spot.ChangeFindMyDeviceSettingsRequest;
import com.google.android.gms.findmydevice.spot.GetCachedDevicesRequest;
import com.google.android.gms.findmydevice.spot.GetFindMyDeviceSettingsRequest;
import com.google.android.gms.findmydevice.spot.GetKeychainLockScreenKnowledgeFactorSupportRequest;
import com.google.android.gms.findmydevice.spot.GetOwnerKeyRequest;
import com.google.android.gms.findmydevice.spot.ImportGivenOwnerKeyRequest;
import com.google.android.gms.findmydevice.spot.ImportRequiredOwnerKeysRequest;
import com.google.android.gms.findmydevice.spot.SetOwnerKeyRequest;
import com.google.android.gms.findmydevice.spot.SyncOwnerKeyRequest;

interface ISpotManagementService {
    void getOwnerKey(ISpotManagementCallbacks callbacks, in GetOwnerKeyRequest request) = 0;
    void setOwnerKey(ISpotManagementCallbacks callbacks, in SetOwnerKeyRequest request) = 1;
    void importRequiredOwnerKeys(ISpotManagementCallbacks callbacks, in ImportRequiredOwnerKeysRequest request) = 2;
    void syncOwnerKey(ISpotManagementCallbacks callbacks, in SyncOwnerKeyRequest request) = 3;
    void importGivenOwnerKey(ISpotManagementCallbacks callbacks, in ImportGivenOwnerKeyRequest request) = 4;
    void getFindMyDeviceSettings(ISpotManagementCallbacks callbacks, in GetFindMyDeviceSettingsRequest request) = 5;
    void changeFindMyDeviceSettings(ISpotManagementCallbacks callbacks, in ChangeFindMyDeviceSettingsRequest request) = 6;
    void getKeychainLockScreenKnowledgeFactorSupport(ISpotManagementCallbacks callbacks, in GetKeychainLockScreenKnowledgeFactorSupportRequest request) = 7;
    void getCachedDevices(ISpotManagementCallbacks callbacks, in GetCachedDevicesRequest request) = 8;
}