/*
 * SPDX-FileCopyrightText: 2023, e Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vending

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
    fun setLicensingEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.LICENSING, enabled)
        }
    }

    @JvmStatic
    fun isLicensingPurchaseFreeAppsEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.LICENSING_PURCHASE_FREE_APPS)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setLicensingPurchaseFreeAppsEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.LICENSING_PURCHASE_FREE_APPS, enabled)
        }
    }

    @JvmStatic
    fun isBillingEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.BILLING)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setBillingEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.BILLING, enabled)
        }
    }
}