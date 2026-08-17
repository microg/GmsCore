/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

/**
 * Exception thrown when the server returns a non-200 response code.
 */
public class HttpStatusException extends Exception {
    private int errorHttpStatus;

    /**
     * Constructs a {@link HttpStatusException} with the specified detail message and error code.
     *
     * @param msg             The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param errorHttpStatus The status code of the failed HTTP request.
     */
    public HttpStatusException(String msg, int errorHttpStatus) {
        super(msg);
        this.errorHttpStatus = errorHttpStatus;
    }

    /**
     * Returns the status code of a failed HTTP request.
     */
    public int getHttpErrorStatus() {
        return errorHttpStatus;
    }
}
