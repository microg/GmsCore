/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import java.io.IOException;

/**
 * Exception thrown when the mobile client fails to connect to the reCAPTCHA server.
 */
public class RecaptchaNetworkException extends Exception {
    /**
     * Constructs a {@link RecaptchaNetworkException} with the specified detail message and {@link IOException}.
     *
     * @param msg The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param e   The root {@link IOException} that causes the {@link RecaptchaNetworkException}.
     */
    public RecaptchaNetworkException(String msg, IOException e) {
        super(msg, e);
    }
}
