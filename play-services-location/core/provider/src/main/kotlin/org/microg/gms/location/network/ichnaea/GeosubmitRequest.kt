/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class GeosubmitRequest(
    val items: List<GeosubmitItem>? = null
)