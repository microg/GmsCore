/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth

import android.os.Bundle
import android.util.Log

private const val TAG = "GmsCarrierAuth"

class CarrierAuthServiceImpl {
    fun asBinder(): Bundle {
        val bundle = Bundle()
        Log.d(TAG, "Providing carrier auth binder")
        return bundle
    }
}
