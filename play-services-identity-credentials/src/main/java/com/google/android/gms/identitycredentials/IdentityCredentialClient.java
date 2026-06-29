/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

/**
 * A client for the Identity Credentials API.
 */
public interface IdentityCredentialClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * Returns a {@link Task} which asynchronously generates a {@link ClearCreationOptionsResponse} on success or throws an {@code ApiException} on failure,
     * when attempting to clear from the creation option registry that should match one registered with {@link IdentityCredentialClient#registerCreationOptions}.
     *
     * @param request informs the type of operation
     */
    @NonNull
    Task<ClearCreationOptionsResponse> clearCreationOptions(@NonNull ClearCreationOptionsRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link ClearCredentialStateResponse} on success or throws an {@code ApiException} on failure,
     * when attempting to clear credential state.
     *
     * @param request specifies the clear credential state request
     */
    @NonNull
    Task<ClearCredentialStateResponse> clearCredentialState(@NonNull ClearCredentialStateRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link ClearExportResponse} on success or throws an {@code ApiException} on failure, when
     * attempting to clear from the registry.
     *
     * @param request informs the type of operation
     */
    @NonNull
    Task<ClearExportResponse> clearExport(@NonNull ClearExportRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link ClearRegistryResponse} on success or throws an {@code ApiException} on failure, when attempting to clear from the registry.
     *
     * @param request informs the type of operation
     */
    @NonNull
    Task<ClearRegistryResponse> clearRegistry(@NonNull ClearRegistryRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link CreateCredentialHandle} on success or throws an {@code ApiException} on failure.
     *
     * @param request containing parameters of the credential to be created
     */
    @NonNull
    Task<CreateCredentialHandle> createCredential(@NonNull CreateCredentialRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link PendingGetCredentialHandle} on success or throws an {@code ApiException} on failure.
     *
     * @param request the request for getting the credential
     */
    @NonNull
    Task<PendingGetCredentialHandle> getCredential(@NonNull GetCredentialRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link PendingImportCredentialsHandle} on success or throws an {@code ApiException} on failure.
     *
     * @param request the information needed to import credentials from another provider
     */
    @NonNull
    Task<PendingImportCredentialsHandle> importCredentials(@NonNull ImportCredentialsRequest request);

    /**
     * Register the creation options that may serve a {@link IdentityCredentialClient#createCredential} transaction.
     * <p>
     * Returns a {@link Task} which asynchronously generates a {@link RegisterCreationOptionsResponse} on success or throws an {@code ApiException} on
     * failure, when attempting to write to the registry.
     *
     * @param request specifies the credential information being written to the registry
     */
    @NonNull
    Task<RegisterCreationOptionsResponse> registerCreationOptions(@NonNull RegisterCreationOptionsRequest request);

    /**
     * RRegister the credential options that may serve a {@link IdentityCredentialClient#getCredential} transaction.
     * <p>
     * Returns a {@link Task} which asynchronously generates a {@link RegistrationResponse} on success or throws an {@code ApiException} on failure, when
     * attempting to write to the registry.
     *
     * @param request specifies the credential information being written to the registry
     */
    @NonNull
    Task<RegistrationResponse> registerCredentials(@NonNull RegistrationRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link RegisterExportResponse} on success or throws an {@code ApiException} on failure, when
     * attempting to write to the registry.
     *
     * @param request specifies the information being written to the registry
     */
    @NonNull
    Task<RegisterExportResponse> registerExport(@NonNull RegisterExportRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link SignalCredentialStateResponse} on success or throws an {@code ApiException} on failure,
     * when attempting to signal providers with credential state.
     *
     * @param request specifies the signal credential state request
     */
    @NonNull
    Task<SignalCredentialStateResponse> signalCredentialState(@NonNull SignalCredentialStateRequest request);
}
