/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class ResponseLocation(
    val latitude: Double,
    val longitude: Double,

    // Custom
    val horizontalAccuracy: Double? = null,
    val altitude: Double? = null,
    val verticalAccuracy: Double? = null
)