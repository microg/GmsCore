/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.os.Bundle
import android.util.Log

private const val TAG = "GmsRcs"

class RcsServiceImpl {
    fun asBinder(): Bundle {
        val bundle = Bundle()
        Log.d(TAG, "Providing RCS binder")
        return bundle
    }
}
