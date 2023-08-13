/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.mozilla

enum class RadioType {
    GSM, WCDMA, LTE;

    override fun toString(): String {
        return super.toString().lowercase()
    }
}