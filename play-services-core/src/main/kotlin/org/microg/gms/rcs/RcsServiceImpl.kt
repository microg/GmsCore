/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.os.Bundle

class RcsServiceImpl {
    fun asBinder(): Bundle {
        val bundle = Bundle()
        bundle.putString("rcs_service_version", "1")
        return bundle
    }
}
