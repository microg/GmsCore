/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity.internal;

import com.google.android.gms.auth.api.identity.internal.IAuthorizationCallback;
import com.google.android.gms.auth.api.identity.internal.IVerifyWithGoogleCallback;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.VerifyWithGoogleRequest;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.auth.api.identity.ClearTokenRequest;
import com.google.android.gms.common.api.internal.IStatusCallback;

interface IAuthorizationService {
    void authorize(in IAuthorizationCallback callback, in AuthorizationRequest request) = 0;
    void verifyWithGoogle(in IVerifyWithGoogleCallback callback, in VerifyWithGoogleRequest request) = 1;
    void revokeAccess(in IStatusCallback callback, in RevokeAccessRequest request) = 2;
    void clearToken(in IStatusCallback callback, in ClearTokenRequest request) = 3;
}