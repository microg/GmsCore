/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.wifi

data class WifiDetails(
    val macAddress: String,
    val ssid: String? = null,
    val timestamp: Long? = null,
    val frequency: Int? = null,
    val channel: Int? = null,
    val signalStrength: Int? = null,
    val open: Boolean = false
)
