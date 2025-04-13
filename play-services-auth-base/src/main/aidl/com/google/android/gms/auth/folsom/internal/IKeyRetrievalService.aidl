/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.folsom.internal;

import com.google.android.gms.auth.folsom.RecoveryRequest;
import com.google.android.gms.auth.folsom.SharedKey;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalConsentCallback;
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalSyncStatusCallback;
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalCallback;
import com.google.android.gms.auth.folsom.internal.ISharedKeyCallback;
import com.google.android.gms.auth.folsom.internal.IStringListCallback;
import com.google.android.gms.auth.folsom.internal.IRecoveryResultCallback;
import com.google.android.gms.auth.folsom.internal.IByteArrayListCallback;
import com.google.android.gms.auth.folsom.internal.IByteArrayCallback;
import com.google.android.gms.auth.folsom.internal.ISecurityDomainMembersCallback;
import com.google.android.gms.auth.folsom.internal.IBooleanCallback;
import com.google.android.gms.common.api.internal.IStatusCallback;

interface IKeyRetrievalService {
    void setConsent(in IKeyRetrievalConsentCallback callback, String accountName, boolean force, in ApiMetadata metadata) = 0;
    void getConsent(in IKeyRetrievalConsentCallback callback, String accountName, in ApiMetadata metadata) = 1;
    void getSyncStatus(in IKeyRetrievalSyncStatusCallback callback, String accountName, in ApiMetadata metadata) = 2;
    void markLocalKeysAsStale(in IKeyRetrievalCallback callback, String accountName, in ApiMetadata metadata) = 3;
    void getKeyMaterial(in ISharedKeyCallback callback, String accountName, in ApiMetadata metadata) = 4;
    void setKeyMaterial(in IKeyRetrievalCallback callback, String accountName, in SharedKey[] keys, in ApiMetadata metadata) = 5;
    void getRecoveredSecurityDomains(in IStringListCallback callback, String accountName, in ApiMetadata metadata) = 6;
    void startRecoveryOperation(in IRecoveryResultCallback callback, in ApiMetadata metadata, in RecoveryRequest request) = 7;
    void listVaultsOperation(in IByteArrayListCallback callback, String accountName, in ApiMetadata metadata) = 8;
    void getProductDetails(in IByteArrayCallback callback, String accountName, in ApiMetadata metadata) = 9;
    void joinSecurityDomain(in IStatusCallback callback, String accountName, in byte[] bytes, int type, in ApiMetadata metadata) = 10;
    void startUxFlow(in IKeyRetrievalCallback callback, String accountName, int type, in ApiMetadata metadata) = 11;
    void promptForLskfConsent(in IKeyRetrievalCallback callback, String accountName, in ApiMetadata metadata) = 12;
    void resetSecurityDomain(in IStatusCallback callback, String accountName, in ApiMetadata metadata) = 13;
    void listSecurityDomainMembers(in ISecurityDomainMembersCallback callback, String accountName, in ApiMetadata metadata) = 14;
    void generateOpenVaultRequestOperation(in IByteArrayCallback callback, in RecoveryRequest request, in ApiMetadata metadata) = 15;
    void canSilentlyAddGaiaPassword(in IBooleanCallback callback, String accountName, in ApiMetadata metadata) = 16;
    void addGaiaPasswordMember(in IStatusCallback callback, String accountName, in ApiMetadata metadata) = 17;
}
