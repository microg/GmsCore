/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.findmydevice.spot.ChangeFindMyDeviceSettingsResponse;
import com.google.android.gms.findmydevice.spot.GetCachedDevicesResponse;
import com.google.android.gms.findmydevice.spot.GetFindMyDeviceSettingsResponse;
import com.google.android.gms.findmydevice.spot.GetKeychainLockScreenKnowledgeFactorSupportResponse;
import com.google.android.gms.findmydevice.spot.GetOwnerKeyResponse;
import com.google.android.gms.findmydevice.spot.ImportGivenOwnerKeyResponse;
import com.google.android.gms.findmydevice.spot.ImportRequiredOwnerKeysResponse;
import com.google.android.gms.findmydevice.spot.SetOwnerKeyResponse;
import com.google.android.gms.findmydevice.spot.SyncOwnerKeyResponse;

interface ISpotManagementCallbacks {
    void onGetOwnerKey(in Status status, in GetOwnerKeyResponse response) = 0;
    void onSetOwnerKey(in Status status, in SetOwnerKeyResponse response) = 1;
    void onImportRequiredOwnerKeys(in Status status, in ImportRequiredOwnerKeysResponse response) = 2;
    void onSyncOwnerKey(in Status status, in SyncOwnerKeyResponse response) = 3;
    void onImportGivenOwnerKey(in Status status, in ImportGivenOwnerKeyResponse response) = 4;
    void onGetFindMyDeviceSettings(in Status status, in GetFindMyDeviceSettingsResponse response) = 5;
    void onChangeFindMyDeviceSettings(in Status status, in ChangeFindMyDeviceSettingsResponse response) = 6;
    void onGetKeychainLockScreenKnowledgeFactorSupport(in Status status, in GetKeychainLockScreenKnowledgeFactorSupportResponse response) = 7;
    void onGetCachedDevices(in Status status, in GetCachedDevicesResponse response) = 8;
}



