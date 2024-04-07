/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

enum class GeosubmitSource {
    GPS, MANUAL, FUSED;

    override fun toString(): String {
        return super.toString().lowercase()
    }
}