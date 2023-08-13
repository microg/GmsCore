/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.wifi

import android.content.Context
import android.os.WorkSource

interface WifiDetailsSource {
    fun enable() = Unit
    fun disable() = Unit
    fun startScan(workSource: WorkSource?) = Unit

    companion object {
        fun create(context: Context, callback: WifiDetailsCallback) = when {
            WifiScannerSource.isSupported(context) -> WifiScannerSource(context, callback)
            else -> WifiManagerSource(context, callback)
        }
    }
}