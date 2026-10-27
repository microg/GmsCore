/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.signature.fake

import android.content.Context

object FakeSignatureManager {
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun enableForPackage(context: Context, packageName: String) {
        // The signature policy database is only used by Huawei builds.
    }
}
