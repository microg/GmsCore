/*
 * SPDX-FileCopyrightText: 2023, e Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending

import android.content.Context
import org.microg.gms.settings.SettingsContract

object VendingPreferences {
    @JvmStatic
    fun isLicensingEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.LICENSING)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun isBillingEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.BILLING)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }
}