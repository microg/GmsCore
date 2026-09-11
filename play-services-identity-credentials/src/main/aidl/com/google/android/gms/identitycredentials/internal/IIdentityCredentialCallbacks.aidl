/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identitycredentials.ClearCreationOptionsResponse;
import com.google.android.gms.identitycredentials.ClearCredentialStateResponse;
import com.google.android.gms.identitycredentials.ClearExportResponse;
import com.google.android.gms.identitycredentials.ClearRegistryResponse;
import com.google.android.gms.identitycredentials.CreateCredentialHandle;
import com.google.android.gms.identitycredentials.CreateCredentialResponse;
import com.google.android.gms.identitycredentials.CredentialInformationResponse;
import com.google.android.gms.identitycredentials.CredentialTransferCapabilities;
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupResponse;
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupResponse;
import com.google.android.gms.identitycredentials.PendingGetCredentialHandle;
import com.google.android.gms.identitycredentials.PendingImportCredentialsHandle;
import com.google.android.gms.identitycredentials.RegisterCreationOptionsResponse;
import com.google.android.gms.identitycredentials.RegisterExportResponse;
import com.google.android.gms.identitycredentials.RegistrationResponse;
import com.google.android.gms.identitycredentials.SignalCredentialStateResponse;

interface IIdentityCredentialCallbacks {
    void onGetCredential(in Status status, in PendingGetCredentialHandle handle, in ApiMetadata apiMetadata) = 0;
    void onRegister(in Status status, in RegistrationResponse response, in ApiMetadata apiMetadata) = 1;
    void onClearRegistry(in Status status, in ClearRegistryResponse response, in ApiMetadata apiMetadata) = 2;
    void onImportCredentials(in Status status, in PendingImportCredentialsHandle handle, in ApiMetadata apiMetadata) = 3;
    void onRegisterExport(in Status status, in RegisterExportResponse response, in ApiMetadata apiMetadata) = 4;
    void onCreateCredentialLegacy(in Status status, in CreateCredentialResponse response, in ApiMetadata apiMetadata) = 5;
    void onCreateCredential(in Status status, in CreateCredentialHandle handle, in ApiMetadata apiMetadata) = 6;
    void onRegisterCreationOptions(in Status status, in RegisterCreationOptionsResponse response, in ApiMetadata apiMetadata) = 7;
    void onClearCredentialState(in Status status, in ClearCredentialStateResponse response, in ApiMetadata apiMetadata) = 8;
    void onSignalCredentialState(in Status status, in SignalCredentialStateResponse response, in ApiMetadata apiMetadata) = 9;
    void onClearExport(in Status status, in ClearExportResponse response, in ApiMetadata apiMetadata) = 10;
    void onImportCredentialsForDeviceSetup(in Status status, in ImportCredentialsForDeviceSetupResponse response, in ApiMetadata apiMetadata) = 11;
    void onExportCredentialsToDeviceSetup(in Status status, in ExportCredentialsToDeviceSetupResponse response, in ApiMetadata apiMetadata) = 12;
    void onGetCredentialTransferCapabilities(in Status status, in CredentialTransferCapabilities capabilities, in ApiMetadata apiMetadata) = 13;
    void onClearCreationOptions(in Status status, in ClearCreationOptionsResponse response, in ApiMetadata apiMetadata) = 14;
    void onGetCredentialInformation(in Status status, in CredentialInformationResponse response) = 15;
}
