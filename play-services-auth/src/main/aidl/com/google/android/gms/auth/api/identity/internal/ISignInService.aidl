/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.auth.api.identity.internal.IBeginSignInCallback;
import com.google.android.gms.auth.api.identity.internal.IGetSignInIntentCallback;
import com.google.android.gms.auth.api.identity.internal.IGetPhoneNumberHintIntentCallback;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest;
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest;

interface ISignInService {
    void beginSignIn(in IBeginSignInCallback callback, in BeginSignInRequest request) = 0;
    void signOut(in IStatusCallback callback, String userId) = 1;
    void getSignInIntent(in IGetSignInIntentCallback callback, in GetSignInIntentRequest request) = 2;
    void getPhoneNumberHintIntent(in IGetPhoneNumberHintIntentCallback callback, in GetPhoneNumberHintIntentRequest request) = 3;
}