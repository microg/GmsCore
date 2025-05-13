/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.threadnetwork.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;

import com.google.android.gms.threadnetwork.ThreadBorderAgent;
import com.google.android.gms.threadnetwork.ThreadNetworkCredentials;

import com.google.android.gms.threadnetwork.internal.IGetCredentialsByExtendedPanIdCallback;
import com.google.android.gms.threadnetwork.internal.IGetPreferredCredentialsCallback;
import com.google.android.gms.threadnetwork.internal.IIsPreferredCredentialsCallback;
import com.google.android.gms.threadnetwork.internal.IThreadNetworkServiceCallbacks;

interface IThreadNetworkService {
    void addCredentials(IStatusCallback callback, in ThreadBorderAgent borderAgent, in ThreadNetworkCredentials credentials) = 0;
    void removeCredentials(IStatusCallback callback, in ThreadBorderAgent borderAgent) = 1;
    void getAllCredentials(IThreadNetworkServiceCallbacks callbacks) = 3;
    void getCredentialsByExtendedPanId(IGetCredentialsByExtendedPanIdCallback callback, in byte[] extendedPanId) = 4;
    void getCredentialsByBorderAgent(IThreadNetworkServiceCallbacks callbacks, in ThreadBorderAgent borderAgent) = 5;
    void getPreferredCredentials(IGetPreferredCredentialsCallback callback) = 7;
    void isPreferredCredentials(IIsPreferredCredentialsCallback callback, in ThreadNetworkCredentials credentials) = 8;
}