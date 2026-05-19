/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
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
     * Returns a {@link Task} which asynchronously generates a {@link ClearCreationOptionsResponse} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the clear creation options operation.
     */
    @NonNull
    Task<ClearCreationOptionsResponse> clearCreationOptions(@NonNull ClearCreationOptionsRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link ClearCredentialStateResponse} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the clear credential state operation.
     */
    @NonNull
    Task<ClearCredentialStateResponse> clearCredentialState(@NonNull ClearCredentialStateRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link ClearExportResponse} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the clear export operation.
     */
    @NonNull
    Task<ClearExportResponse> clearExport(@NonNull ClearExportRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link ClearRegistryResponse} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the clear registry operation.
     */
    @NonNull
    Task<ClearRegistryResponse> clearRegistry(@NonNull ClearRegistryRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link CreateCredentialHandle} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the create credential operation.
     */
    @NonNull
    Task<CreateCredentialHandle> createCredential(@NonNull CreateCredentialRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link PendingGetCredentialHandle} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the get credential operation.
     */
    @NonNull
    Task<PendingGetCredentialHandle> getCredential(@NonNull GetCredentialRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link PendingImportCredentialsHandle} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the import credentials operation.
     */
    @NonNull
    Task<PendingImportCredentialsHandle> importCredentials(@NonNull ImportCredentialsRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link RegisterCreationOptionsResponse} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the register creation options operation.
     */
    @NonNull
    Task<RegisterCreationOptionsResponse> registerCreationOptions(@NonNull RegisterCreationOptionsRequest request);

    /**
     * Register the credential options described in the given {@link RegistrationRequest}.
     *
     * @param request configuration for the register credentials operation.
     * @return a {@link Task} which asynchronously generates a {@link RegistrationResponse} on success, or throws an
     *         {@code OperationException} on failure.
     */
    @NonNull
    Task<RegistrationResponse> registerCredentials(@NonNull RegistrationRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link RegisterExportResponse} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the register export operation.
     */
    @NonNull
    Task<RegisterExportResponse> registerExport(@NonNull RegisterExportRequest request);

    /**
     * Returns a {@link Task} which asynchronously generates a {@link SignalCredentialStateResponse} on success, or throws an
     * {@code OperationException} on failure.
     *
     * @param request configuration for the signal credential state operation.
     */
    @NonNull
    Task<SignalCredentialStateResponse> signalCredentialState(@NonNull SignalCredentialStateRequest request);
}
