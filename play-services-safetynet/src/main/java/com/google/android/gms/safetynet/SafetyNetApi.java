/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.safetynet;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Response;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

/**
 * The main entry point for interacting with SafetyNet.
 */
public interface SafetyNetApi {
    /**
     * Provides user attestation with reCAPTCHA.
     * <p>
     * If reCAPTCHA is confident that this is a real user on a real device it will return a token with no challenge.
     * Otherwise it will provide a visual/audio challenge to attest the humanness of the user before returning a token.
     * <p>
     * When you make a request with this API, you must provide your client {@link GoogleApiClient} and site public key
     * as parameters, and after the request completes, you can get the {@link RecaptchaTokenResult}
     * from the response.
     *
     * @param client  The {@link GoogleApiClient} to service the call. The client must be connected using
     *                {@link GoogleApiClient#connect()} before invoking this method.
     * @param siteKey A site public key registered for this app
     * @deprecated use {@link SafetyNetClient#verifyWithRecaptcha(String)}
     */
    @Deprecated
    PendingResult<RecaptchaTokenResult> verifyWithRecaptcha(GoogleApiClient client, String siteKey);

    /**
     * Response from {@link SafetyNetClient#attest(byte[], String)} that contains a Compatibility Test Suite
     * attestation result.
     * <p>
     * Use the {@link Result#getStatus()} method to obtain a {@link Status} object. Calling the Status object's
     * {@link Status#isSuccess} indicates whether or not communication with the service was successful, but does not
     * indicate if the device has passed the compatibility check. If the request was successful,
     * {@link AttestationResponse#getJwsResult()} may be used to determine whether the device has passed the
     * compatibility check.
     */
    class AttestationResponse extends Response<AttestationResult> {
        /**
         * Gets the JSON Web Signature attestation result.
         * <p>
         * Result is in JSON Web Signature format.
         *
         * @return JSON Web signature formatted attestation response or {@code null} if an error occurred.
         */
        public String getJwsResult() {
            return getResult().getJwsResult();
        }
    }

    @PublicApi(exclude = true)
    @Deprecated
    interface AttestationResult extends Result {
        String getJwsResult();
    }

    /**
     * {@link Response} from {@link SafetyNetClient#verifyWithRecaptcha(String)}.
     * <p>
     * This Result contains a reCAPTCHA user response token and third party clients should use this token to verify
     * the user. The token must be validated on the server side to determine whether the user has passed the challenge.
     */
    class RecaptchaTokenResponse extends Response<RecaptchaTokenResult> {
        /**
         * Gets the reCAPTCHA user response token which must be validated by calling the siteverify method.
         *
         * @return A user response token.
         */
        public String getTokenResult() {
            return getResult().getTokenResult();
        }
    }

    /**
     * A Result from {@link #verifyWithRecaptcha(GoogleApiClient, String)}.
     * <p>
     * This Result contains a reCAPTCHA user response token and third party clients should use this token to verify
     * the user. Calling the {@link Status#isSuccess()} will indicate whether or not communication with the service was
     * successful, but does not indicate if the user has passed the reCAPTCHA challenge. The token must be validated on
     * the server side to determine whether the user has passed the challenge.
     *
     * @deprecated use {@link RecaptchaTokenResponse} returned from {@link SafetyNetClient#verifyWithRecaptcha(String)}.
     */
    @Deprecated
    interface RecaptchaTokenResult extends Result {
        String getTokenResult();
    }
}
