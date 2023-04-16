/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import org.microg.safeparcel.SafeParcelable;

/**
 * Stores the information required to verify an account.
 * <p>
 * This object is only valid for a specific time after creation and it holds all the information needed to validate a
 * PIN using {@link RecaptchaClient#verifyAccount(String, VerificationHandle)}. If an expired handle is returned by
 * {@link RecaptchaClient#challengeAccount(RecaptchaHandle, String)}, then {@link #getOperationAbortedToken()} will
 * return a token that can be used with the reCAPTCHA Enterprise API CreateAssessment() to get more details.
 */
public abstract class VerificationHandle implements SafeParcelable {
    /**
     * Returns the length of the PIN code.
     */
    public abstract int getCodeLength();

    /**
     * Returns a reCAPTCHA token in the case {@link RecaptchaClient#challengeAccount(RecaptchaHandle, String)}
     * operation was aborted and an expired {@link VerificationHandle} was returned, otherwise this returns an empty
     * string. This can be used with the reCAPTCHA Enterprise API CreateAssessment() to retrieve more details.
     */
    public abstract String getOperationAbortedToken();

    /**
     * Returns the site public key you registered for using reCAPTCHA.
     */
    public abstract String getSiteKey();

    /**
     * Returns the validity duration of the object since its creation in minutes.
     */
    public abstract long getTimeoutMinutes();

    /**
     * Returns an encrypted version of the internal verification token that will be used in
     * {@link RecaptchaClient#verifyAccount(String, VerificationHandle)} call.
     */
    public abstract String getVerificationToken();

    /**
     * Returns a boolean indicating if the {@link VerificationHandle} is valid for
     * {@link RecaptchaClient#verifyAccount(String, VerificationHandle)} API calls. An invalid handle will cause
     * {@link RecaptchaClient#verifyAccount(String, VerificationHandle)} calls to fail immediately.
     */
    public boolean isValid() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
