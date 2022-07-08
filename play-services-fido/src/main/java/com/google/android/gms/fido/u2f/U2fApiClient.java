/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.u2f;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.fido.Fido;
import com.google.android.gms.tasks.Task;

/**
 * The entry point for interacting with the regular app U2F APIs.
 * <p>
 * U2F (Universal Second Factor) is the name of the Security Key protocol in FIDO (Fast IDentity Online), which is the
 * industry alliance where Security Keys are being standardized.
 *
 * @deprecated Please use {@link Fido} APIs instead.
 */
@Deprecated
public class U2fApiClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    private static final Api<Api.ApiOptions.NoOptions> API = null;

    /**
     * @param activity Calling {@link Activity}
     */
    public U2fApiClient(Activity activity) {
        super(activity, API);
        throw new UnsupportedOperationException();
    }

    /**
     * @param context The {@link Context} of the calling application
     */
    public U2fApiClient(Context context) {
        super(context, API);
        throw new UnsupportedOperationException();
    }

//    /**
//     * Creates a Task with {@link U2fPendingIntent}. When this Task object starts, it issues a U2F registration request,
//     * which is done once per U2F device per account for associating the new U2F device with that account.
//     *
//     * @param requestParams for the registration request
//     * @return Task with PendingIntent to launch U2F registration request
//     */
//    public Task<U2fPendingIntent> getRegisterIntent(RegisterRequestParams requestParams) {
//        throw new UnsupportedOperationException();
//    }

//    /**
//     * Creates a Task with U2fPendingIntent. When this Task object starts, it issues a U2F signing request for a relying party to authenticate a user.
//     *
//     * @param requestParams for the sign request
//     * @return Task with PendingIntent to launch U2F signature request
//     */
//    public Task<U2fPendingIntent> getSignIntent(SignRequestParams requestParams) {
//        throw new UnsupportedOperationException();
//    }
}
