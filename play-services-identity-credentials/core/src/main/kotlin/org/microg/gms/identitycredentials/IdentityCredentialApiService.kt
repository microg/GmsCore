/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.identitycredentials

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.identitycredentials.ClearCreationOptionsRequest
import com.google.android.gms.identitycredentials.ClearCredentialStateRequest
import com.google.android.gms.identitycredentials.ClearCredentialStateResponse
import com.google.android.gms.identitycredentials.ClearExportRequest
import com.google.android.gms.identitycredentials.ClearRegistryRequest
import com.google.android.gms.identitycredentials.CreateCredentialHandle
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.CredentialInformation
import com.google.android.gms.identitycredentials.CredentialInformationRequest
import com.google.android.gms.identitycredentials.CredentialInformationResponse
import com.google.android.gms.identitycredentials.CredentialTransferCapabilities
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupRequest
import com.google.android.gms.identitycredentials.GetCredentialRequest
import com.google.android.gms.identitycredentials.GetCredentialTransferCapabilitiesRequest
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupRequest
import com.google.android.gms.identitycredentials.ImportCredentialsRequest
import com.google.android.gms.identitycredentials.PendingGetCredentialHandle
import com.google.android.gms.identitycredentials.RegisterCreationOptionsRequest
import com.google.android.gms.identitycredentials.RegisterExportRequest
import com.google.android.gms.identitycredentials.RegistrationRequest
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialCallbacks
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialService
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.AccountUtils
import org.microg.gms.common.GmsService
import org.microg.gms.fido.core.Database
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager

private const val TAG = "IdentityCredentialApi"

private const val CHOOSER_ACTIVITY_CLASS = "org.microg.gms.auth.credentials.identity.IdentityCredentialChooserActivity"

const val EXTRA_GET_REQUEST = "org.microg.gms.identitycredentials.EXTRA_GET_REQUEST"
const val EXTRA_CREATE_REQUEST = "org.microg.gms.identitycredentials.EXTRA_CREATE_REQUEST"
const val EXTRA_CALLING_PACKAGE = "org.microg.gms.identitycredentials.EXTRA_CALLING_PACKAGE"

private val FEATURES = arrayOf(
    Feature("GET_CREDENTIAL", 1),
    Feature("CREDENTIAL_REGISTRY", 1),
    Feature("CLEAR_REGISTRY", 2),
    Feature("CLEAR_CREATION_OPTIONS", 1),
    Feature("GET_CREDENTIAL_INFORMATION", 1),
    Feature("CLEAR_CREDENTIAL_STATE", 1),
    Feature("CREATE_CREDENTIAL", 3),
    Feature("REGISTER_CREATION_OPTIONS", 1),
    Feature("REGISTER_EXPORT", 1),
    Feature("IMPORT_CREDENTIALS", 1),
    Feature("SIGNAL_CREDENTIAL_STATE", 1),
    Feature("CLEAR_EXPORT", 1),
    Feature("IMPORT_CREDENTIALS_FOR_DEVICE_SETUP", 3),
    Feature("EXPORT_CREDENTIALS_TO_DEVICE_SETUP", 3),
    Feature("GET_CREDENTIAL_TRANSFER_CAPABILITIES", 3),
)

class IdentityCredentialApiService : BaseService(TAG, GmsService.IDENTITY_CREDENTIALS) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest pkg=${request.packageName}")
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = FEATURES
        ProfileManager.ensureInitialized(this)
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            IdentityCredentialApiServiceImpl(this, request.packageName, lifecycle).asBinder(),
            connectionInfo
        )
    }
}

class IdentityCredentialApiServiceImpl(
    private val context: Context,
    private val clientPackageName: String,
    override val lifecycle: Lifecycle,
) : IIdentityCredentialService.Stub(), LifecycleOwner {

    override fun getCredential(callback: IIdentityCredentialCallbacks, request: GetCredentialRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "getCredential pkg=$clientPackageName options=${request.credentialOptions.size} origin=${request.origin}")
        callback.onGetCredential(Status.SUCCESS, PendingGetCredentialHandle(buildChooserPendingIntent(request)), ApiMetadata.SKIP)
    }

    override fun createCredential(callback: IIdentityCredentialCallbacks, request: CreateCredentialRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "createCredential pkg=$clientPackageName type=${request.type} origin=${request.origin}")
        callback.onCreateCredential(Status.SUCCESS, CreateCredentialHandle(buildCreateChooserPendingIntent(request), null), ApiMetadata.SKIP)
    }

    override fun clearCredentialState(callback: IIdentityCredentialCallbacks, request: ClearCredentialStateRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "clearCredentialState pkg=$clientPackageName")
        callback.onClearCredentialState(Status.SUCCESS, ClearCredentialStateResponse(Bundle.EMPTY), ApiMetadata.SKIP)
    }

    override fun getCredentialInformation(callback: IIdentityCredentialCallbacks, request: CredentialInformationRequest, apiMetadata: ApiMetadata) {
        val packageNames = request.packageNames.orEmpty()
        Log.d(TAG, "getCredentialInformation pkg=$clientPackageName count=${packageNames.size}")
        if (Build.VERSION.SDK_INT < 34) {
            callback.onGetCredentialInformation(Status.SUCCESS, CredentialInformationResponse(emptyList()))
            return
        }
        lifecycleScope.launch {
            fun resolveCredentialInformation(packageName: String): CredentialInformation {
                val installed = runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
                if (!installed) return CredentialInformation(packageName, 0, 0, 0, 0)
                val hasPasskey = runCatching { Database(context).getKnownRegistrationInfo(packageName).isNotEmpty() }.getOrDefault(false)
                val hasGoogleAccount = AccountUtils.get(context).getSelectedAccount(packageName) != null
                return CredentialInformation(packageName, 0, if (hasPasskey) 1 else 0, if (hasGoogleAccount) 1 else 0, 0)
            }
            val infos = packageNames.filterNotNull().map { resolveCredentialInformation(it) }
            runCatching { callback.onGetCredentialInformation(Status.SUCCESS, CredentialInformationResponse(infos)) }
                .onFailure { Log.w(TAG, "getCredentialInformation callback failed", it) }
        }
    }

    override fun register(callback: IIdentityCredentialCallbacks, request: RegistrationRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "register: not implemented")
        callback.onRegister(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun clearRegistry(callback: IIdentityCredentialCallbacks, request: ClearRegistryRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "clearRegistry: not implemented")
        callback.onClearRegistry(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun importCredentials(callback: IIdentityCredentialCallbacks, request: ImportCredentialsRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "importCredentials: not implemented")
        callback.onImportCredentials(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun registerExport(callback: IIdentityCredentialCallbacks, request: RegisterExportRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "registerExport: not implemented")
        callback.onRegisterExport(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun registerCreationOptions(callback: IIdentityCredentialCallbacks, request: RegisterCreationOptionsRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "registerCreationOptions: not implemented")
        callback.onRegisterCreationOptions(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun signalCredentialState(callback: IIdentityCredentialCallbacks, request: SignalCredentialStateRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "signalCredentialState: not implemented")
        callback.onSignalCredentialState(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun clearExport(callback: IIdentityCredentialCallbacks, request: ClearExportRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "clearExport: not implemented")
        callback.onClearExport(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun importCredentialsForDeviceSetup(callback: IIdentityCredentialCallbacks, request: ImportCredentialsForDeviceSetupRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "importCredentialsForDeviceSetup: not implemented")
        callback.onImportCredentialsForDeviceSetup(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun exportCredentialsToDeviceSetup(callback: IIdentityCredentialCallbacks, request: ExportCredentialsToDeviceSetupRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "exportCredentialsToDeviceSetup: not implemented")
        callback.onExportCredentialsToDeviceSetup(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    override fun getCredentialTransferCapabilities(callback: IIdentityCredentialCallbacks, request: GetCredentialTransferCapabilitiesRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "getCredentialTransferCapabilities pkg=$clientPackageName")
        callback.onGetCredentialTransferCapabilities(Status.SUCCESS, CredentialTransferCapabilities(Bundle.EMPTY), ApiMetadata.SKIP)
    }

    override fun clearCreationOptions(callback: IIdentityCredentialCallbacks, request: ClearCreationOptionsRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "clearCreationOptions: not implemented")
        callback.onClearCreationOptions(Status(CommonStatusCodes.API_NOT_CONNECTED), null, ApiMetadata.SKIP)
    }

    private fun buildChooserPendingIntent(request: GetCredentialRequest): PendingIntent =
        buildChooserPendingIntent(request.hashCode()) { putExtra(EXTRA_GET_REQUEST, request) }

    private fun buildCreateChooserPendingIntent(request: CreateCredentialRequest): PendingIntent =
        buildChooserPendingIntent(request.hashCode()) { putExtra(EXTRA_CREATE_REQUEST, request) }

    private inline fun buildChooserPendingIntent(requestCode: Int, configure: Intent.() -> Unit): PendingIntent {
        val intent = Intent().apply {
            component = ComponentName(context.packageName, CHOOSER_ACTIVITY_CLASS)
            putExtra(EXTRA_CALLING_PACKAGE, clientPackageName)
            configure()
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
        return PendingIntentCompat.getActivity(context, requestCode, intent, flags, true)!!
    }

}
