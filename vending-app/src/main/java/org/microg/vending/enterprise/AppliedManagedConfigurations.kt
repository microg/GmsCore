/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.content.Context
import org.microg.vending.enterprise.proto.ManagedConfiguration

class AppliedManagedConfigurations(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isApplied(packageName: String, configuration: ManagedConfiguration): Boolean =
        preferences.getString(packageName, null) == configuration.fingerprint()

    fun record(packageName: String, configuration: ManagedConfiguration) {
        preferences.edit().putString(packageName, configuration.fingerprint()).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun ManagedConfiguration.fingerprint(): String =
        encodeByteString().sha256().base64()

    companion object {
        private const val PREFERENCES_NAME = "applied_managed_configurations"
    }
}
