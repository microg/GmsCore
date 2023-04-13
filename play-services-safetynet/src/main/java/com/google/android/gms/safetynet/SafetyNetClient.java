/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.safetynet;

import android.content.Context;
import android.os.RemoteException;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.PendingGoogleApiCall;
import org.microg.gms.safetynet.ISafetyNetCallbacksDefaultStub;
import org.microg.gms.safetynet.SafetyNetGmsClient;

/**
 * The main entry point for SafetyNet.
 */
@PublicApi
public class SafetyNetClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    @PublicApi(exclude = true)
    SafetyNetClient(Context context) {
        super(context, SafetyNet.API);
    }

    /**
     * Provides attestation results for the device.
     * <p>
     * An attestation result states whether the device where it is running matches the profile of a device that has
     * passed Android compatibility testing.
     * <p>
     * When you request a compatibility check, you must provide a nonce, which is a random token generated in a
     * cryptographically secure manner. You can obtain a nonce by generating one within your app each time you make a
     * compatibility check request. As a more secure option, you can obtain a nonce from your own server, using a
     * secure connection.
     * <p>
     * A nonce used with an attestation request should be at least 16 bytes in length. After you make a request, the
     * response {@link SafetyNetApi.AttestationResponse} includes your nonce, so you can verify it against the one you
     * sent. You should only use a nonce value once, for a single request. Use a different nonce for any subsequent
     * attestation requests.
     *
     * @param nonce  A cryptographic nonce used for anti-replay and tracking of requests.
     * @param apiKey An Android API key obtained through the developer console.
     */
    public Task<SafetyNetApi.AttestationResponse> attest(byte[] nonce, String apiKey) {
        return scheduleTask((PendingGoogleApiCall<SafetyNetApi.AttestationResponse, SafetyNetGmsClient>) (client, completionSource) -> {
            try {
                client.attest(new ISafetyNetCallbacksDefaultStub() {
                    @Override
                    public void onAttestationData(Status status, AttestationData attestationData) throws RemoteException {
                        SafetyNetApi.AttestationResponse response = new SafetyNetApi.AttestationResponse();
                        response.setResult(new SafetyNetApi.AttestationResult() {
                            @Override
                            public String getJwsResult() {
                                return attestationData.getJwsResult();
                            }

                            @Override
                            public Status getStatus() {
                                return status;
                            }
                        });
                        completionSource.setResult(response);
                    }
                }, nonce, apiKey);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }

    /**
     * Provides user attestation with reCAPTCHA.
     * <p>
     * If reCAPTCHA is confident that this is a real user on a real device it will return a token with no challenge.
     * Otherwise it will provide a visual/audio challenge to attest the humanness of the user before returning a token.
     *
     * @param siteKey A site public key registered for this app
     */
    public Task<SafetyNetApi.RecaptchaTokenResponse> verifyWithRecaptcha(String siteKey) {
        return scheduleTask((PendingGoogleApiCall<SafetyNetApi.RecaptchaTokenResponse, SafetyNetGmsClient>) (client, completionSource) -> {
            try {
                client.verifyWithRecaptcha(new ISafetyNetCallbacksDefaultStub() {
                    @Override
                    public void onRecaptchaResult(Status status, RecaptchaResultData recaptchaResultData) throws RemoteException {
                        SafetyNetApi.RecaptchaTokenResponse response = new SafetyNetApi.RecaptchaTokenResponse();
                        response.setResult(new SafetyNetApi.RecaptchaTokenResult() {
                            @Override
                            public String getTokenResult() {
                                if (recaptchaResultData != null) {
                                    return recaptchaResultData.token;
                                } else {
                                    return null;
                                }
                            }

                            @Override
                            public Status getStatus() {
                                return status;
                            }
                        });
                        completionSource.setResult(response);

                    }
                }, siteKey);
            } catch (Exception e) {
                completionSource.setException(e);
            }
        });
    }
}
