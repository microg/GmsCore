/*
 * SPDX-FileCopyrightText: 2023, e Foundation
 * SPDX-FileCopyrightText: 2024 microG Project Team
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
    fun isLicensingPurchaseFreeAppsEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.LICENSING_PURCHASE_FREE_APPS)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun isSplitInstallEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.SPLIT_INSTALL)
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

    @JvmStatic
    fun isAssetDeliveryEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.ASSET_DELIVERY)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
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
    fun isInstallEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Vending.APPS_INSTALL)
        return SettingsContract.getSettings(context, SettingsContract.Vending.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
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
    fun isDeviceAttestationEnabled(context: Context): Boolean {
        return SettingsContract.getSettings(context, SettingsContract.SafetyNet.getContentUri(context), SettingsContract.SafetyNet.PROJECTION) { c ->
            c.getInt(0) != 0
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