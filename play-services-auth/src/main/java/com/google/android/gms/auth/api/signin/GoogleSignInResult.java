/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import org.microg.gms.common.Hide;

/**
 * GoogleSignInResults are {@link Result} implementations that potentially contain a {@link GoogleSignInAccount}.
 *
 * @deprecated Use Credential Manager for authentication or Google Identity Services for authorization.
 */
@Deprecated
public class GoogleSignInResult implements Result {
    @Nullable
    private final GoogleSignInAccount signInAccount;
    @NonNull
    private final Status status;

    @Hide
    public GoogleSignInResult(@Nullable GoogleSignInAccount signInAccount, @NonNull Status status) {
        this.signInAccount = signInAccount;
        this.status = status;
    }

    /**
     * Returns a {@link GoogleSignInAccount} reflecting the user's sign in information if sign-in completed successfully; or {@code null} when failed.
     */
    @Nullable
    public GoogleSignInAccount getSignInAccount() {
        return signInAccount;
    }

    /**
     * Returns a {@link Status} object indicating the status of the sign in attempt.
     * <p>
     * You can use {@link #isSuccess()} to determine quickly if sign-in succeeded. If sign-in failed, you can match the status code retrieved from
     * {@link Status#getStatusCode()} to consts defined in {@link GoogleSignInStatusCodes} and its parent class.
     */
    @NonNull
    @Override
    public Status getStatus() {
        return status;
    }

    /**
     * Convenient method to help you tell if sign-in completed successfully.
     */
    public boolean isSuccess() {
        return status.isSuccess();
    }
}
