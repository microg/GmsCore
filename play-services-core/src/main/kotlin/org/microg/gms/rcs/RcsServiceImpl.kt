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
        bundle.putString("rcs_service_version_name", "microg-rcs-1.0")
        bundle.putBoolean("rcs_capabilities_provisioned", true)
        bundle.putBoolean("rcs_capabilities_available", true)
        bundle.putBoolean("rcs_chat_enabled", true)
        bundle.putInt("rcs_sdk_int", 1)
        return bundle
    }
}
