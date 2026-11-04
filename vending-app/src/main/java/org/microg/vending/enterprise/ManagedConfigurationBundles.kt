/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.os.Bundle
import android.util.Log
import org.microg.vending.enterprise.proto.ManagedConfiguration
import org.microg.vending.enterprise.proto.ManagedConfigurationEntry

private const val TAG = "GmsVendingManagedConfig"

internal fun ManagedConfiguration.toBundle(): Bundle = entries.toBundle()

private fun List<ManagedConfigurationEntry>.toBundle(): Bundle {
    val bundle = Bundle(size)
    forEach { it.putInto(bundle) }

    return bundle
}

private fun ManagedConfigurationEntry.putInto(bundle: Bundle) {
    when {
        boolValue != null -> bundle.putBoolean(key, boolValue)
        intValue != null -> bundle.putInt(key, intValue)
        stringValue != null -> bundle.putString(key, stringValue)
        stringListValue != null -> bundle.putStringArray(key, stringListValue.values.toTypedArray())
        bundleValue != null -> bundle.putBundle(key, bundleValue.entries.toBundle())
        bundleArrayValue != null -> bundle.putParcelableArray(key, bundleArrayValue.bundles.map { it.entries.toBundle() }.toTypedArray())
        else -> Log.w(TAG, "Unknown managed configuration value type for key $key")
    }
}
