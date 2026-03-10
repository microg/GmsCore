/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin;

import com.google.android.gms.common.api.Api;
import org.microg.gms.common.Hide;
import org.microg.gms.signin.SignInClientImpl;

@Hide
public class SignIn {
    public static final Api<SignInOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new SignInClientImpl(context, clientSettings, callbacks, connectionFailedListener));
}
