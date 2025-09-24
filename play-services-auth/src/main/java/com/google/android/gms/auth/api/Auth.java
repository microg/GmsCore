/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api;

import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import org.microg.gms.auth.api.signin.GoogleSignInApiImpl;
import org.microg.gms.auth.api.signin.GoogleSignInGmsClientImpl;

/**
 * Entry point for Google Auth APIs through GoogleApiClient.
 */
public class Auth {
    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable the Google Sign In API.
     */
    public static final Api<GoogleSignInOptions> GOOGLE_SIGN_IN_API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new GoogleSignInGmsClientImpl(context, callbacks, connectionFailedListener));

    /**
     * Api entry point for Google Sign In.
     */
    public static final GoogleSignInApi GoogleSignInApi = new GoogleSignInApiImpl();
}
