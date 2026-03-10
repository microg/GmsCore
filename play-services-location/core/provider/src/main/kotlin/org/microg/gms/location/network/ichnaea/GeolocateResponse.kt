/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class GeolocateResponse(
    val location: ResponseLocation? = null,
    val horizontalAccuracy: Double? = null,
    val fallback: String? = null,
    val error: ResponseError? = null,

    // Custom
    val verticalAccuracy: Double? = null,
    val raw: List<RawGeolocateEntry> = emptyList(),
)