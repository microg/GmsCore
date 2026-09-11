/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.identitycredentials;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identitycredentials.ClearCreationOptionsRequest;
import com.google.android.gms.identitycredentials.ClearCreationOptionsResponse;
import com.google.android.gms.identitycredentials.ClearCredentialStateRequest;
import com.google.android.gms.identitycredentials.ClearCredentialStateResponse;
import com.google.android.gms.identitycredentials.ClearExportRequest;
import com.google.android.gms.identitycredentials.ClearExportResponse;
import com.google.android.gms.identitycredentials.ClearRegistryRequest;
import com.google.android.gms.identitycredentials.ClearRegistryResponse;
import com.google.android.gms.identitycredentials.CreateCredentialHandle;
import com.google.android.gms.identitycredentials.CreateCredentialRequest;
import com.google.android.gms.identitycredentials.CreateCredentialResponse;
import com.google.android.gms.identitycredentials.CredentialInformationResponse;
import com.google.android.gms.identitycredentials.CredentialTransferCapabilities;
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupResponse;
import com.google.android.gms.identitycredentials.GetCredentialRequest;
import com.google.android.gms.identitycredentials.IdentityCredentialClient;
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupResponse;
import com.google.android.gms.identitycredentials.ImportCredentialsRequest;
import com.google.android.gms.identitycredentials.PendingGetCredentialHandle;
import com.google.android.gms.identitycredentials.PendingImportCredentialsHandle;
import com.google.android.gms.identitycredentials.RegisterCreationOptionsRequest;
import com.google.android.gms.identitycredentials.RegisterCreationOptionsResponse;
import com.google.android.gms.identitycredentials.RegisterExportRequest;
import com.google.android.gms.identitycredentials.RegisterExportResponse;
import com.google.android.gms.identitycredentials.RegistrationRequest;
import com.google.android.gms.identitycredentials.RegistrationResponse;
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest;
import com.google.android.gms.identitycredentials.SignalCredentialStateResponse;
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialCallbacks;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import org.microg.gms.common.api.PendingGoogleApiCall;

public class IdentityCredentialClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements IdentityCredentialClient {
    public IdentityCredentialClientImpl(Context context) {
        super(context, IdentityCredentialApiClient.API, Api.ApiOptions.NO_OPTIONS);
    }

    @NonNull
    @Override
    public Task<ClearCreationOptionsResponse> clearCreationOptions(@NonNull ClearCreationOptionsRequest request) {
        return scheduleTask((PendingGoogleApiCall<ClearCreationOptionsResponse, IdentityCredentialApiClient>) (client, source) ->
                client.clearCreationOptions(new BaseCallbacks() {
                    @Override
                    public void onClearCreationOptions(Status status, ClearCreationOptionsResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<ClearCredentialStateResponse> clearCredentialState(@NonNull ClearCredentialStateRequest request) {
        return scheduleTask((PendingGoogleApiCall<ClearCredentialStateResponse, IdentityCredentialApiClient>) (client, source) ->
                client.clearCredentialState(new BaseCallbacks() {
                    @Override
                    public void onClearCredentialState(Status status, ClearCredentialStateResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<ClearExportResponse> clearExport(@NonNull ClearExportRequest request) {
        return scheduleTask((PendingGoogleApiCall<ClearExportResponse, IdentityCredentialApiClient>) (client, source) ->
                client.clearExport(new BaseCallbacks() {
                    @Override
                    public void onClearExport(Status status, ClearExportResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<ClearRegistryResponse> clearRegistry(@NonNull ClearRegistryRequest request) {
        return scheduleTask((PendingGoogleApiCall<ClearRegistryResponse, IdentityCredentialApiClient>) (client, source) ->
                client.clearRegistry(new BaseCallbacks() {
                    @Override
                    public void onClearRegistry(Status status, ClearRegistryResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<CreateCredentialHandle> createCredential(@NonNull CreateCredentialRequest request) {
        return scheduleTask((PendingGoogleApiCall<CreateCredentialHandle, IdentityCredentialApiClient>) (client, source) ->
                client.createCredential(new BaseCallbacks() {
                    @Override
                    public void onCreateCredential(Status status, CreateCredentialHandle handle, ApiMetadata apiMetadata) {
                        complete(source, status, handle);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<PendingGetCredentialHandle> getCredential(@NonNull GetCredentialRequest request) {
        return scheduleTask((PendingGoogleApiCall<PendingGetCredentialHandle, IdentityCredentialApiClient>) (client, source) ->
                client.getCredential(new BaseCallbacks() {
                    @Override
                    public void onGetCredential(Status status, PendingGetCredentialHandle handle, ApiMetadata apiMetadata) {
                        complete(source, status, handle);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<PendingImportCredentialsHandle> importCredentials(@NonNull ImportCredentialsRequest request) {
        return scheduleTask((PendingGoogleApiCall<PendingImportCredentialsHandle, IdentityCredentialApiClient>) (client, source) ->
                client.importCredentials(new BaseCallbacks() {
                    @Override
                    public void onImportCredentials(Status status, PendingImportCredentialsHandle handle, ApiMetadata apiMetadata) {
                        complete(source, status, handle);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<RegisterCreationOptionsResponse> registerCreationOptions(@NonNull RegisterCreationOptionsRequest request) {
        return scheduleTask((PendingGoogleApiCall<RegisterCreationOptionsResponse, IdentityCredentialApiClient>) (client, source) ->
                client.registerCreationOptions(new BaseCallbacks() {
                    @Override
                    public void onRegisterCreationOptions(Status status, RegisterCreationOptionsResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<RegistrationResponse> registerCredentials(@NonNull RegistrationRequest request) {
        return scheduleTask((PendingGoogleApiCall<RegistrationResponse, IdentityCredentialApiClient>) (client, source) ->
                client.registerCredentials(new BaseCallbacks() {
                    @Override
                    public void onRegister(Status status, RegistrationResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<RegisterExportResponse> registerExport(@NonNull RegisterExportRequest request) {
        return scheduleTask((PendingGoogleApiCall<RegisterExportResponse, IdentityCredentialApiClient>) (client, source) ->
                client.registerExport(new BaseCallbacks() {
                    @Override
                    public void onRegisterExport(Status status, RegisterExportResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    @NonNull
    @Override
    public Task<SignalCredentialStateResponse> signalCredentialState(@NonNull SignalCredentialStateRequest request) {
        return scheduleTask((PendingGoogleApiCall<SignalCredentialStateResponse, IdentityCredentialApiClient>) (client, source) ->
                client.signalCredentialState(new BaseCallbacks() {
                    @Override
                    public void onSignalCredentialState(Status status, SignalCredentialStateResponse response, ApiMetadata apiMetadata) {
                        complete(source, status, response);
                    }
                }, request));
    }

    private static <T> void complete(TaskCompletionSource<T> source, Status status, T result) {
        if (status != null && status.isSuccess()) {
            source.trySetResult(result);
        } else {
            source.trySetException(new ApiException(status == null ? Status.INTERNAL_ERROR : status));
        }
    }

    // Empty defaults so each scheduleTask can override only the relevant callback.
    private static abstract class BaseCallbacks extends IIdentityCredentialCallbacks.Stub {
        @Override public void onGetCredential(Status status, PendingGetCredentialHandle handle, ApiMetadata apiMetadata) {}
        @Override public void onRegister(Status status, RegistrationResponse response, ApiMetadata apiMetadata) {}
        @Override public void onClearRegistry(Status status, ClearRegistryResponse response, ApiMetadata apiMetadata) {}
        @Override public void onImportCredentials(Status status, PendingImportCredentialsHandle handle, ApiMetadata apiMetadata) {}
        @Override public void onRegisterExport(Status status, RegisterExportResponse response, ApiMetadata apiMetadata) {}
        @Override public void onCreateCredentialLegacy(Status status, CreateCredentialResponse response, ApiMetadata apiMetadata) {}
        @Override public void onCreateCredential(Status status, CreateCredentialHandle handle, ApiMetadata apiMetadata) {}
        @Override public void onRegisterCreationOptions(Status status, RegisterCreationOptionsResponse response, ApiMetadata apiMetadata) {}
        @Override public void onClearCredentialState(Status status, ClearCredentialStateResponse response, ApiMetadata apiMetadata) {}
        @Override public void onSignalCredentialState(Status status, SignalCredentialStateResponse response, ApiMetadata apiMetadata) {}
        @Override public void onClearExport(Status status, ClearExportResponse response, ApiMetadata apiMetadata) {}
        @Override public void onImportCredentialsForDeviceSetup(Status status, ImportCredentialsForDeviceSetupResponse response, ApiMetadata apiMetadata) {}
        @Override public void onExportCredentialsToDeviceSetup(Status status, ExportCredentialsToDeviceSetupResponse response, ApiMetadata apiMetadata) {}
        @Override public void onGetCredentialTransferCapabilities(Status status, CredentialTransferCapabilities capabilities, ApiMetadata apiMetadata) {}
        @Override public void onClearCreationOptions(Status status, ClearCreationOptionsResponse response, ApiMetadata apiMetadata) {}
        @Override public void onGetCredentialInformation(Status status, CredentialInformationResponse response) {}
    }
}
