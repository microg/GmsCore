/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

data class DeviceInfo(val txPowerCorrection: Int, val rssiCorrection: Int)

// TODO
val currentDeviceInfo: DeviceInfo
    get() = DeviceInfo(-17, -5)
