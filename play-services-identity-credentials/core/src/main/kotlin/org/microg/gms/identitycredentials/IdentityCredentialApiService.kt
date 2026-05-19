/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.identitycredentials

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.identitycredentials.ClearCreationOptionsRequest
import com.google.android.gms.identitycredentials.ClearCredentialStateRequest
import com.google.android.gms.identitycredentials.ClearExportRequest
import com.google.android.gms.identitycredentials.ClearRegistryRequest
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.CredentialInformationRequest
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupRequest
import com.google.android.gms.identitycredentials.GetCredentialRequest
import com.google.android.gms.identitycredentials.GetCredentialTransferCapabilitiesRequest
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupRequest
import com.google.android.gms.identitycredentials.ImportCredentialsRequest
import com.google.android.gms.identitycredentials.RegisterCreationOptionsRequest
import com.google.android.gms.identitycredentials.RegisterExportRequest
import com.google.android.gms.identitycredentials.RegistrationRequest
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialCallbacks
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "IdentityCredentialApi"

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

    override fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest?, service: GmsService?) {
        callback?.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            IdentityCredentialApiServiceImpl(lifecycle).asBinder(),
            ConnectionInfo().apply { features = FEATURES }
        )
    }
}

class IdentityCredentialApiServiceImpl(override val lifecycle: Lifecycle) : IIdentityCredentialService.Stub(), LifecycleOwner {

    override fun clearCreationOptions(callbacks: IIdentityCredentialCallbacks?, request: ClearCreationOptionsRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "clearCreationOptions: not implemented")
        callbacks?.onClearCreationOptions(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun clearCredentialState(callbacks: IIdentityCredentialCallbacks?, request: ClearCredentialStateRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "clearCredentialState: not implemented")
        callbacks?.onClearCredentialState(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun clearExport(callbacks: IIdentityCredentialCallbacks?, request: ClearExportRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "clearExport: not implemented")
        callbacks?.onClearExport(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun clearRegistry(callbacks: IIdentityCredentialCallbacks?, request: ClearRegistryRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "clearRegistry: not implemented")
        callbacks?.onClearRegistry(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun createCredential(callbacks: IIdentityCredentialCallbacks?, request: CreateCredentialRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "createCredential: not implemented")
        callbacks?.onCreateCredential(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun exportCredentialsToDeviceSetup(callbacks: IIdentityCredentialCallbacks?, request: ExportCredentialsToDeviceSetupRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "exportCredentialsToDeviceSetup: not implemented")
        callbacks?.onExportCredentialsToDeviceSetup(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun getCredential(callbacks: IIdentityCredentialCallbacks?, request: GetCredentialRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getCredential: not implemented, request=$request")
        callbacks?.onGetCredential(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun getCredentialInformation(callbacks: IIdentityCredentialCallbacks?, request: CredentialInformationRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getCredentialInformation: not implemented")
        callbacks?.onGetCredentialInformation(Status.INTERNAL_ERROR, null)
    }

    override fun getCredentialTransferCapabilities(callbacks: IIdentityCredentialCallbacks?, request: GetCredentialTransferCapabilitiesRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getCredentialTransferCapabilities: not implemented")
        callbacks?.onGetCredentialTransferCapabilities(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun importCredentials(callbacks: IIdentityCredentialCallbacks?, request: ImportCredentialsRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "importCredentials: not implemented")
        callbacks?.onImportCredentials(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun importCredentialsForDeviceSetup(callbacks: IIdentityCredentialCallbacks?, request: ImportCredentialsForDeviceSetupRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "importCredentialsForDeviceSetup: not implemented")
        callbacks?.onImportCredentialsForDeviceSetup(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun registerCreationOptions(callbacks: IIdentityCredentialCallbacks?, request: RegisterCreationOptionsRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "registerCreationOptions: not implemented")
        callbacks?.onRegisterCreationOptions(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun register(callbacks: IIdentityCredentialCallbacks?, request: RegistrationRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "register: not implemented")
        callbacks?.onRegister(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun registerExport(callbacks: IIdentityCredentialCallbacks?, request: RegisterExportRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "registerExport: not implemented")
        callbacks?.onRegisterExport(Status.INTERNAL_ERROR, null, apiMetadata)
    }

    override fun signalCredentialState(callbacks: IIdentityCredentialCallbacks?, request: SignalCredentialStateRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "signalCredentialState: not implemented")
        callbacks?.onSignalCredentialState(Status.INTERNAL_ERROR, null, apiMetadata)
    }
}
