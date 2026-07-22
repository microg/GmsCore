/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.account.data;

import com.google.android.gms.auth.firstparty.dataservice.DeviceManagementInfoResponse;
import com.google.android.gms.common.api.Status;

interface IDeviceManagementInfoCallback {
    void onResult(in Status status, in DeviceManagementInfoResponse response);
}
