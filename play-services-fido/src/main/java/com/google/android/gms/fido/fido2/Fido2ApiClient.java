/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.microg.gms.common.PublicApi;
import org.microg.gms.fido.fido2.Fido2PendingIntentImpl;

/**
 * The entry point for interacting with FIDO2 APIs.
 */
@PublicApi
public class Fido2ApiClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    private static final Api<Api.ApiOptions.NoOptions> API = null;

    @PublicApi(exclude = true)
    public Fido2ApiClient(Context context) {
        super(context, API);
    }

    /**
     * @deprecated use {@link #getRegisterPendingIntent(PublicKeyCredentialCreationOptions)} instead
     */
    @Deprecated
    public Task<Fido2PendingIntent> getRegisterIntent(PublicKeyCredentialCreationOptions requestOptions) {
        return getRegisterPendingIntent(requestOptions).onSuccessTask(pendingIntent -> Tasks.forResult(new Fido2PendingIntentImpl(pendingIntent)));
    }

    /**
     * Creates a Task with {@link PendingIntent}, when started, will issue a FIDO2 registration request, which is done
     * once per FIDO2 device per account for associating the new FIDO2 device with that account.
     *
     * @param requestOptions for the registration request
     * @return Task with PendingIntent to launch FIDO2 registration request
     */
    public Task<PendingIntent> getRegisterPendingIntent(PublicKeyCredentialCreationOptions requestOptions) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated use {@link #getSignPendingIntent(PublicKeyCredentialRequestOptions)} instead
     */
    @Deprecated
    public Task<Fido2PendingIntent> getSignIntent(PublicKeyCredentialRequestOptions requestOptions) {
        return getSignPendingIntent(requestOptions).onSuccessTask(pendingIntent -> Tasks.forResult(new Fido2PendingIntentImpl(pendingIntent)));
    }

    /**
     * Creates a Task with {@link PendingIntent}, when started, will issue a FIDO2 signature request for a relying
     * party to authenticate a user.
     *
     * @param requestOptions for the sign request
     * @return Task with PendingIntent to launch FIDO2 signature request
     */
    public Task<PendingIntent> getSignPendingIntent(PublicKeyCredentialRequestOptions requestOptions) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a Task with {@link Boolean}, which check if a user verifying platform authenticator is available on the
     * device.
     */
    public Task<Boolean> isUserVerifyingPlatformAuthenticatorAvailable() {
        throw new UnsupportedOperationException();
    }
}
