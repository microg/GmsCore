/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.util.Log
import com.android.vending.buildRequestHeaders
import org.microg.vending.billing.core.AuthData
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_MANAGED_CONFIGURATIONS
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.proto.GoogleApiResponse
import org.microg.vending.enterprise.proto.ManagedConfiguration
import org.microg.vending.enterprise.proto.ManagedConfigurationRequestApp
import org.microg.vending.enterprise.proto.ManagedConfigurationsRequest

internal class ManagedConfigurationsRepository {

    private val client = HttpClient()

    suspend fun fetchConfiguration(auth: AuthData, app: App): ManagedConfiguration? {
        val configurations = client.post(
            url = URL_MANAGED_CONFIGURATIONS,
            headers = buildRequestHeaders(auth.authToken, auth.gsfId.toLong(16)),
            payload = ManagedConfigurationsRequest(
                apps = listOf(ManagedConfigurationRequestApp(packageName = app.packageName, versionCode = app.versionCode))
            ),
            adapter = GoogleApiResponse.ADAPTER
        ).payload?.managedConfigurationsResponse?.configurations.orEmpty()

        Log.v(TAG, "managed configurations: ${configurations.joinToString { "${it.packageName}: ${it.entries.size} entries" }}")
        return configurations.find { it.packageName == app.packageName }
    }

    companion object {
        private const val TAG = "GmsVendingManagedConfig"
    }
}
