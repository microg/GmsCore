/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.expressintegrityservice
data class DeviceIntegrityResponse(
    var deviceIntegrity: DeviceIntegrity, var attemptedDroidGuardTokenRefresh: Boolean, var deviceKeyMd5: String, var expiredDeviceKey: Any?
)
