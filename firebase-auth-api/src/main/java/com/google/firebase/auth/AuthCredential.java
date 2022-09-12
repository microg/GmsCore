/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.firebase.auth;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Represents a credential that the Firebase Authentication server can use to authenticate a user.
 */
@PublicApi
public abstract class AuthCredential extends AutoSafeParcelable {
    /**
     * Returns the unique string identifier for the provider type with which the credential is associated.
     */
    public abstract String getProvider();

    /**
     * Returns the unique string identifier for the sign in method with which the credential is associated. Should match that returned by {@link FirebaseAuth#fetchSignInMethodsForEmail(String)} after this user has signed in with this type of credential.
     */
    public abstract String getSignInMethod();
}
