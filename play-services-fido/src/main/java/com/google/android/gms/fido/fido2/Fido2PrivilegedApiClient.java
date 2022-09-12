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
import android.os.RemoteException;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fido.fido2.api.IBooleanCallback;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.internal.privileged.IFido2PrivilegedCallbacks;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.ApiClient;
import org.microg.gms.common.api.PendingGoogleApiCall;
import org.microg.gms.fido.fido2.Fido2PendingIntentImpl;
import org.microg.gms.fido.fido2.Fido2PrivilegedGmsClient;

/**
 * The entry point for interacting with the privileged FIDO2 APIs.
 */
@PublicApi
public class Fido2PrivilegedApiClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    private static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new Fido2PrivilegedGmsClient(context, callbacks, connectionFailedListener));

    @PublicApi(exclude = true)
    public Fido2PrivilegedApiClient(Context context) {
        super(context, API);
    }

    /**
     * Creates a Task with PendingIntent, when started, will issue a FIDO2 registration request for privileged apps.
     *
     * @param requestOptions for the registration request from a Web browser
     * @return PendingResult with PendingIntent to launch FIDO2 registration request
     * @deprecated use {@link #getRegisterPendingIntent(BrowserPublicKeyCredentialCreationOptions)} instead
     */
    @Deprecated
    public Task<Fido2PendingIntent> getRegisterIntent(BrowserPublicKeyCredentialCreationOptions requestOptions) {
        return getRegisterPendingIntent(requestOptions).onSuccessTask(pendingIntent -> Tasks.forResult(new Fido2PendingIntentImpl(pendingIntent)));
    }

    /**
     * Creates a Task with PendingIntent, when started, will issue a FIDO2 registration request for privileged apps.
     *
     * @param requestOptions for the registration request from a Web browser
     * @return PendingResult with PendingIntent to launch FIDO2 registration request
     */
    public Task<PendingIntent> getRegisterPendingIntent(BrowserPublicKeyCredentialCreationOptions requestOptions) {
        return scheduleTask((PendingGoogleApiCall<PendingIntent, Fido2PrivilegedGmsClient>) (client, completionSource) -> {
            try {
                client.register(new IFido2PrivilegedCallbacks.Stub() {
                    @Override
                    public void onPendingIntent(Status status, PendingIntent pendingIntent) throws RemoteException {
                        if (status.isSuccess()) {
                            completionSource.setResult(pendingIntent);
                        } else {
                            completionSource.setException(new ApiException(status));
                        }
                    }
                }, requestOptions);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    /**
     * Creates a Task with PendingIntent, when started, will issue a FIDO2 signature request for privileged apps.
     *
     * @param requestOptions for the sign request from a Web browser
     * @return PendingResult with PendingIntent to launch FIDO2 signature request
     * @deprecated use {@link #getSignPendingIntent(BrowserPublicKeyCredentialRequestOptions)} instead
     */
    @Deprecated
    public Task<Fido2PendingIntent> getSignIntent(BrowserPublicKeyCredentialRequestOptions requestOptions) {
        return getSignPendingIntent(requestOptions).onSuccessTask(pendingIntent -> Tasks.forResult(new Fido2PendingIntentImpl(pendingIntent)));
    }

    /**
     * Creates a Task with PendingIntent, when started, will issue a FIDO2 signature request for privileged apps.
     *
     * @param requestOptions for the sign request from a Web browser
     * @return PendingResult with PendingIntent to launch FIDO2 signature request
     */
    public Task<PendingIntent> getSignPendingIntent(BrowserPublicKeyCredentialRequestOptions requestOptions) {
        return scheduleTask((PendingGoogleApiCall<PendingIntent, Fido2PrivilegedGmsClient>) (client, completionSource) -> {
            try {
                client.sign(new IFido2PrivilegedCallbacks.Stub() {
                    @Override
                    public void onPendingIntent(Status status, PendingIntent pendingIntent) throws RemoteException {
                        if (status.isSuccess()) {
                            completionSource.setResult(pendingIntent);
                        } else {
                            completionSource.setException(new ApiException(status));
                        }
                    }
                }, requestOptions);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    /**
     * Creates a Task with {@link Boolean}, which check if a user verifying platform authenticator is available on the
     * device.
     */
    public Task<Boolean> isUserVerifyingPlatformAuthenticatorAvailable() {
        return scheduleTask((PendingGoogleApiCall<Boolean, Fido2PrivilegedGmsClient>) (client, completionSource) -> {
            try {
                client.isUserVerifyingPlatformAuthenticatorAvailable(new IBooleanCallback.Stub() {
                    @Override
                    public void onBoolean(boolean value) throws RemoteException {
                        completionSource.setResult(value);
                    }

                    @Override
                    public void onError(Status status) throws RemoteException {
                        completionSource.setException(new ApiException(status));
                    }
                });
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }
}
