/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

/**
 * The main entry point for interacting with the reCAPTCHA API.
 */
public interface RecaptchaClient {
    /**
     * Sends a challenge to an account in order to verify the identity of the user.
     * <p>
     * This method can be optionally called if you decide to perform a two factor authentication check on an account.
     *
     * @param recaptchaHandle       RecaptchaHandle initialized through {@link #init(String)}.
     * @param challengeRequestToken The challenge request token obtained through CreateAssessment().
     * @return A VerificationHandle that can be used with {@link #verifyAccount(String, VerificationHandle)} calls. A
     * handle is usually valid for a specific time after creation. If an expired handle is returned, meaning the
     * operation was aborted, {@link VerificationHandle#getOperationAbortedToken()} will return a token that can be
     * used with the reCAPTCHA Enterprise API CreateAssessment() to get more details.
     */
    Task<VerificationHandle> challengeAccount(RecaptchaHandle recaptchaHandle, String challengeRequestToken);

    /**
     * Closes the initialized RecaptchaHandle.
     * <p>
     * Closes the handle if you will not be using it again to save resources.
     *
     * @param handle RecaptchaHandle initialized through {@link #init(String)}.
     * @return true if the handle got closed successfully for the first time, false if there wasn't anything to close
     * (e.g., the handle was already previously closed).
     */
    Task<Boolean> close(RecaptchaHandle handle);

    /**
     * Returns a score indicating how likely the action was triggered by a real user. A score close to 0 indicates a likely bot, and a score close to 1 indicates a likely human.
     * <p>
     * This method should be called every time there is an action to be protected.
     *
     * @param handle {@link RecaptchaHandle} initialized through {@link #init(String)}.
     * @param action User interaction that needs to be protected.
     */
    Task<RecaptchaResultData> execute(RecaptchaHandle handle, RecaptchaAction action);

    /**
     * Prepares and initializes a RecaptchaHandle.
     *
     * @param siteKey A site key registered for this app
     */
    Task<RecaptchaHandle> init(String siteKey);

    /**
     * Verifies a PIN against a verification handle obtained through a
     * {@link #challengeAccount(RecaptchaHandle, String)} call.
     * <p>
     * The method should be called to verify a PIN submitted by the user. The returned {@link VerificationResult} will
     * contain a Status and either a {@link VerificationHandle} or a new reCAPTCHA token.
     *
     * @param pin                The fixed-length numerical PIN entered by the user. If a PIN with unexpected length or
     *                           non numerical characters is entered, a
     *                           {@link RecaptchaStatusCodes#RECAPTCHA_2FA_INVALID_PIN} error will be returned.
     * @param verificationHandle A verification handle containing information required to preform the verification
     *                           operation.
     * @return A {@link VerificationResult} containing a {@link Status} indicating the status of the of the verification.
     */
    Task<VerificationResult> verifyAccount(String pin, VerificationHandle verificationHandle);
}
