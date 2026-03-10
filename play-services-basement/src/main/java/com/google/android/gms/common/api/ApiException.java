/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import org.microg.gms.common.PublicApi;

/**
 * Exception to be returned by a Task when a call to Google Play services has failed.
 */
@PublicApi
public class ApiException extends Exception {
    /**
     * @deprecated use {@link #getStatus()} instead
     */
    @PublicApi
    protected final Status mStatus;

    /**
     * Create an ApiException from a {@link com.google.android.gms.common.api.Status}.
     * @param status the Status instance containing a message and code.
     */
    @PublicApi
    public ApiException(Status status) {
        mStatus = status;
    }

    /**
     * Returns the status of the operation.
     */
    @PublicApi
    public Status getStatus() {
        return mStatus;
    }

    /**
     * Indicates the status of the operation.
     * @return Status code resulting from the operation.
     * The value is one of the constants in {@link com.google.android.gms.common.api.CommonStatusCodes} or specific to the API in use.
     */
    @PublicApi
    public int getStatusCode() {
        return mStatus.getStatusCode();
    }

    /**
     * @deprecated use {@link #getMessage()} for a summary of the cause.
     */
    @PublicApi
    public String getStatusMessage() {
        return getMessage();
    }

    @Override
    public String getMessage() {
        return mStatus.getStatusMessage();
    }
}
