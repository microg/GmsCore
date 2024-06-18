/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.internal.regular;

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.api.IBooleanCallback;
import com.google.android.gms.fido.fido2.internal.regular.IFido2AppCallbacks;

interface IFido2AppService {
    void nativeAppRegister(IFido2AppCallbacks callback, in PublicKeyCredentialCreationOptions options) = 0;
    void nativeAppSignIn(IFido2AppCallbacks callback, in PublicKeyCredentialRequestOptions options) = 1;
    void nativeAppIsUserVerifyingPlatformAuthenticatorAvailable(IBooleanCallback callback) = 2;
    void nativeAppIsUserVerifyingPlatformAuthenticatorAvailableForCredential(IBooleanCallback callback, String rpId, in byte[] keyHandles) = 3;
}
