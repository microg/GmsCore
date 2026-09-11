/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.identitycredentials.ClearCreationOptionsRequest;
import com.google.android.gms.identitycredentials.ClearCredentialStateRequest;
import com.google.android.gms.identitycredentials.ClearExportRequest;
import com.google.android.gms.identitycredentials.ClearRegistryRequest;
import com.google.android.gms.identitycredentials.CreateCredentialRequest;
import com.google.android.gms.identitycredentials.CredentialInformationRequest;
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupRequest;
import com.google.android.gms.identitycredentials.GetCredentialRequest;
import com.google.android.gms.identitycredentials.GetCredentialTransferCapabilitiesRequest;
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupRequest;
import com.google.android.gms.identitycredentials.ImportCredentialsRequest;
import com.google.android.gms.identitycredentials.RegisterCreationOptionsRequest;
import com.google.android.gms.identitycredentials.RegisterExportRequest;
import com.google.android.gms.identitycredentials.RegistrationRequest;
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest;
import com.google.android.gms.identitycredentials.internal.IIdentityCredentialCallbacks;

interface IIdentityCredentialService {
    void getCredential(IIdentityCredentialCallbacks callbacks, in GetCredentialRequest request, in ApiMetadata apiMetadata) = 0;
    void register(IIdentityCredentialCallbacks callbacks, in RegistrationRequest request, in ApiMetadata apiMetadata) = 1;
    void clearRegistry(IIdentityCredentialCallbacks callbacks, in ClearRegistryRequest request, in ApiMetadata apiMetadata) = 2;
    void importCredentials(IIdentityCredentialCallbacks callbacks, in ImportCredentialsRequest request, in ApiMetadata apiMetadata) = 3;
    void registerExport(IIdentityCredentialCallbacks callbacks, in RegisterExportRequest request, in ApiMetadata apiMetadata) = 4;
    void createCredential(IIdentityCredentialCallbacks callbacks, in CreateCredentialRequest request, in ApiMetadata apiMetadata) = 5;
    void registerCreationOptions(IIdentityCredentialCallbacks callbacks, in RegisterCreationOptionsRequest request, in ApiMetadata apiMetadata) = 7;
    void clearCredentialState(IIdentityCredentialCallbacks callbacks, in ClearCredentialStateRequest request, in ApiMetadata apiMetadata) = 8;
    void signalCredentialState(IIdentityCredentialCallbacks callbacks, in SignalCredentialStateRequest request, in ApiMetadata apiMetadata) = 9;
    void clearExport(IIdentityCredentialCallbacks callbacks, in ClearExportRequest request, in ApiMetadata apiMetadata) = 10;
    void importCredentialsForDeviceSetup(IIdentityCredentialCallbacks callbacks, in ImportCredentialsForDeviceSetupRequest request, in ApiMetadata apiMetadata) = 11;
    void exportCredentialsToDeviceSetup(IIdentityCredentialCallbacks callbacks, in ExportCredentialsToDeviceSetupRequest request, in ApiMetadata apiMetadata) = 12;
    void getCredentialTransferCapabilities(IIdentityCredentialCallbacks callbacks, in GetCredentialTransferCapabilitiesRequest request, in ApiMetadata apiMetadata) = 13;
    void clearCreationOptions(IIdentityCredentialCallbacks callbacks, in ClearCreationOptionsRequest request, in ApiMetadata apiMetadata) = 14;
    void getCredentialInformation(IIdentityCredentialCallbacks callbacks, in CredentialInformationRequest request, in ApiMetadata apiMetadata) = 15;
}
