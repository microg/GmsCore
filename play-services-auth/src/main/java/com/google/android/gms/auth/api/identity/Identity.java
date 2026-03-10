/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;

/**
 * The entry point to the Sign-In APIs.
 */
public final class Identity {

//    /**
//     * Returns a new instance of {@link AuthorizationClient}.
//     */
//    @NonNull
//    public static AuthorizationClient getAuthorizationClient(@NonNull Context context) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Returns a new instance of {@link AuthorizationClient}.
//     */
//    @NonNull
//    public static AuthorizationClient getAuthorizationClient(@NonNull Activity activity) {
//        throw new UnsupportedOperationException();
//    }

    /**
     * Returns a new instance of {@link CredentialSavingClient}.
     */
    @NonNull
    public static CredentialSavingClient getCredentialSavingClient(@NonNull Activity activity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a new instance of {@link CredentialSavingClient}.
     */
    @NonNull
    public static CredentialSavingClient getCredentialSavingClient(@NonNull Context context) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a new instance of {@link SignInClient}.
     */
    @NonNull
    public static SignInClient getSignInClient(@NonNull Activity activity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a new instance of {@link SignInClient}.
     */
    @NonNull
    public static SignInClient getSignInClient(@NonNull Context context) {
        throw new UnsupportedOperationException();
    }

    private Identity() {
    }
}
