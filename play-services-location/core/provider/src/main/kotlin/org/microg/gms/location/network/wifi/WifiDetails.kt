/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.wifi

import org.microg.gms.location.network.NetworkDetails

data class WifiDetails(
    val macAddress: String,
    val ssid: String? = null,
    val frequency: Int? = null,
    val channel: Int? = null,
    override val timestamp: Long? = null,
    override val signalStrength: Int? = null,
    val open: Boolean = false
): NetworkDetails
