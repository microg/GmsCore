/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import android.app.Application
import org.microg.gms.profile.ProfileManager

// TODO: Get rid
object ContextProvider {
    lateinit var context: Application
        private set

    fun init(application: Application) {
        context = application
    }
}