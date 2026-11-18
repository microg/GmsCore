/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.accounts.Account
import android.content.Context
import android.content.pm.PackageInfo
import android.util.Base64
import android.util.Log
import com.android.vending.buildRequestHeaders
import org.microg.gms.checkin.DeviceConfig
import org.microg.gms.common.DeviceConfiguration
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.vending.UploadDeviceConfigRequest
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.core.AuthData
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ENTERPRISE_CLIENT_POLICY
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_FDFE
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ITEM_DETAILS
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.createDeviceEnvInfo
import org.microg.vending.billing.proto.GoogleApiResponse
import org.microg.vending.enterprise.proto.AppInstallPolicy
import org.microg.vending.enterprise.proto.ChargingConstraint
import org.microg.vending.enterprise.proto.DeviceIdleConstraint
import org.microg.vending.enterprise.proto.EnterpriseConsistencyToken
import org.microg.vending.enterprise.proto.InstallConstraint
import org.microg.vending.enterprise.proto.NetworkTypeConstraint
import org.microg.vending.enterprise.proto.App as PolicyApp
import org.microg.vending.proto.AppMeta
import org.microg.vending.proto.GetItemsRequest
import org.microg.vending.proto.ItemResponse
import org.microg.vending.proto.RequestApp
import org.microg.vending.proto.RequestItem
import java.util.concurrent.TimeUnit

internal class EnterpriseApps(
    val auth: AuthData,
    val states: Map<EnterpriseApp, AppState>
)

internal class EnterpriseAppsRepository(private val context: Context) {

    private val client = HttpClient()

    suspend fun fetchApps(account: Account): EnterpriseApps? {
        ProfileManager.ensureInitialized(context)

        val auth = AuthManager.getAuthData(context, account)
        if (auth == null || createDeviceEnvInfo(context) == null) {
            Log.e(TAG, "Unable to authenticate against the store with $account")
            return null
        }

        val headers = buildRequestHeaders(auth.authToken, auth.gsfId.toLong(16)) + consistencyTokenHeader(account)
        uploadDeviceConfig(headers)

        val policyApps = fetchPolicyApps(headers)
        if (policyApps.isEmpty()) {
            return EnterpriseApps(auth, emptyMap())
        }

        return EnterpriseApps(auth, fetchStates(headers, policyApps))
    }

    private fun consistencyTokenHeader(account: Account): Map<String, String> {
        val creationTimeMillis = WorkAccountCreationTimes(context).creationTimeMillisOf(account) ?: return emptyMap()
        val token = EnterpriseConsistencyToken(timestampMicros = TimeUnit.MILLISECONDS.toMicros(creationTimeMillis))

        return mapOf(HEADER_CONSISTENCY_TOKEN to Base64.encodeToString(token.encode(), Base64.NO_WRAP))
    }

    private suspend fun uploadDeviceConfig(headers: Map<String, String>) {
        val upload = client.post(
            url = "$URL_FDFE/uploadDeviceConfig",
            headers = headers.minus("X-PS-RH"),
            payload = UploadDeviceConfigRequest(
                DeviceConfiguration(context).asProto(),
                manufacturer = Build.MANUFACTURER,
            ),
            adapter = GoogleApiResponse.ADAPTER
        )
        Log.d(TAG, "uploaddc: ${upload.payload?.uploadDeviceConfigResponse}")
    }

    private suspend fun fetchPolicyApps(headers: Map<String, String>): List<PolicyApp> {
        val policyApps = client.post(
            url = URL_ENTERPRISE_CLIENT_POLICY,
            headers = headers.plus("content-type" to "application/x-protobuf"),
            adapter = GoogleApiResponse.ADAPTER
        ).payload?.enterpriseClientPolicyResponse?.policy?.apps?.filter { it.packageName != null }
            ?: throw IllegalStateException("Enterprise client policy response misses expected fields")

        Log.v(TAG, "app policy: ${policyApps.joinToString { "${it.packageName}: ${it.policy}" }}")
        return policyApps
    }

    private suspend fun fetchStates(
        headers: Map<String, String>,
        policyApps: List<PolicyApp>
    ): Map<EnterpriseApp, AppState> {
        val installedPackages = context.packageManager.getInstalledPackages(0)

        return client.post(
            url = URL_ITEM_DETAILS,
            // TODO: meaning unclear, but returns 400 without. constant? possibly has influence on which fields are returned?
            headers = headers.plus("x-dfe-item-field-mask" to "GgWGHay3ByILPP/Avy+4A4YlCRM"),
            payload = GetItemsRequest(
                policyApps.map { RequestItem(RequestApp(AppMeta(it.packageName))) }
            ),
            adapter = GoogleApiResponse.ADAPTER
        ).getItemsResponses.mapNotNull { it.response }.associate { item ->
            val packageName = item.meta!!.packageName!!
            val installedDetails = installedPackages.find { it.applicationInfo?.packageName == packageName }
            val policyApp = policyApps.find { it.packageName == packageName }!!

            item.toEnterpriseApp(packageName, policyApp) to item.toAppState(installedDetails)
        }.onEach {
            Log.v(TAG, "${it.key.packageName} (state: ${it.value}) delivery token: ${it.key.deliveryToken ?: "none acquired"}")
        }
    }

    private fun ItemResponse.toEnterpriseApp(packageName: String, policyApp: PolicyApp) = EnterpriseApp(
        packageName,
        offer?.delivery?.let { offer.version!!.versionCode!! },
        detail!!.name!!.displayName!!,
        detail.icon?.icon?.paint?.url,
        offer?.delivery?.key,
        offer?.delivery?.dependencies?.map {
            App(it.packageName!!, it.versionCode!!, it.packageName, null, emptyList(), null)
        } ?: emptyList(),
        policyApp.policy ?: AppInstallPolicy.OPTIONAL,
        policyApp.installConstraints.map { it.toInstallConstraints() }.ifEmpty { listOf(InstallConstraints.NONE) },
    )

    private fun InstallConstraint.toInstallConstraints() = InstallConstraints(
        requiresUnmeteredNetwork = networkType == NetworkTypeConstraint.INSTALL_ONLY_ON_UNMETERED_NETWORK,
        requiresCharging = charging == ChargingConstraint.INSTALL_ONLY_WHEN_CHARGING,
        requiresDeviceIdle = deviceIdle == DeviceIdleConstraint.INSTALL_ONLY_WHEN_DEVICE_IDLE
    )

    private fun ItemResponse.toAppState(installedDetails: PackageInfo?): AppState {
        val available = offer?.delivery != null

        return if (!available && installedDetails == null) NotCompatible
        else if (!available) Installed
        else if (installedDetails == null) NotInstalled
        else if (installedDetails.versionCode < offer!!.version!!.versionCode!!) UpdateAvailable
        else Installed
    }

    private fun DeviceConfiguration.asProto(): DeviceConfig = DeviceConfig(
        availableFeature = availableFeatures,
        densityDpi = densityDpi,
        glEsVersion = glEsVersion,
        glExtension = glExtensions,
        hasFiveWayNavigation = hasFiveWayNavigation,
        hasHardKeyboard = hasHardKeyboard,
        heightPixels = heightPixels,
        keyboardType = keyboardType,
        locale = locales,
        nativePlatform = nativePlatforms,
        navigation = navigation,
        screenLayout = screenLayout,
        sharedLibrary = sharedLibraries,
        touchScreen = touchScreen,
        widthPixels = widthPixels
    )

    companion object {
        private const val TAG = "GmsVendingEnterprise"
        private const val HEADER_CONSISTENCY_TOKEN = "X-DFE-Enterprise-AclConsistencyToken"
    }
}
