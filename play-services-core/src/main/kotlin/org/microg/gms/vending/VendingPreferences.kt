/*
 * SPDX-FileCopyrightText: 2023, e Foundation
 * SPDX-FileCopyrightText: 2024 microG Project Team
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
    fun isLicensingSplitInstallEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.SPLIT_INSTALL)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setSplitInstallEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.SPLIT_INSTALL, enabled)
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

    @JvmStatic
    fun isAssetDeliveryEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.ASSET_DELIVERY)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setAssetDeliveryEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.ASSET_DELIVERY, enabled)
        }
    }

    @JvmStatic
    fun isDeviceSyncEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.ASSET_DEVICE_SYNC)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setDeviceSyncEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.ASSET_DEVICE_SYNC, enabled)
        }
    }

    @JvmStatic
    fun isInstallEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.APPS_INSTALL)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setInstallEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.APPS_INSTALL, enabled)
        }
    }

    @JvmStatic
    fun getInstallerList(context: Context): String {
        val projection = arrayOf(SettingsContract.Vending.APPS_INSTALLER_LIST)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getString(0)
        }
    }

    @JvmStatic
    fun setInstallerList(context: Context, content: String) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.APPS_INSTALLER_LIST, content)
        }
    }

    @JvmStatic
    fun getPlayIntegrityAppList(context: Context): String {
        val projection = arrayOf(SettingsContract.Vending.PLAY_INTEGRITY_APP_LIST)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getString(0)
        }
    }

    @JvmStatic
    fun setPlayIntegrityAppList(context: Context, content: String) {
        SettingsContract.setSettings(context, SettingsContract.Vending.getContentUri(context)) {
            put(SettingsContract.Vending.PLAY_INTEGRITY_APP_LIST, content)
        }
    }
}