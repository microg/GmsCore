/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.common.app

class GCoreApplicationContext private constructor() : BaseApplicationContext(null) {

    companion object {
        val instance: GCoreApplicationContext by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            GCoreApplicationContext()
        }
    }
}
