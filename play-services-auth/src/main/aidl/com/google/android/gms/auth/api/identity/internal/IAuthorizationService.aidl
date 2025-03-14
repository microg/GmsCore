/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity.internal;

import com.google.android.gms.auth.api.identity.internal.IAuthorizationCallback;
import com.google.android.gms.auth.api.identity.internal.IVerifyWithGoogleCallback;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.VerifyWithGoogleRequest;

interface IAuthorizationService {
    void authorize(in IAuthorizationCallback callback, in AuthorizationRequest request) = 0;
    void verifyWithGoogle(in IVerifyWithGoogleCallback callback, in VerifyWithGoogleRequest request) = 1;
}