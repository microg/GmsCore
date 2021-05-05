/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.credentials;

import android.accounts.Account;

/**
 * Identity provider constants for use with {@link CredentialRequest.Builder#setAccountTypes(String...)}
 */
public final class IdentityProviders {
    public static final String FACEBOOK = "//www.facebook.com";
    public static final String GOOGLE = "//accounts.google.com";
    public static final String LINKEDIN = "//www.linkedin.com";
    public static final String MICROSOFT = "//login.live.com";
    public static final String PAYPAL = "//www.paypal.com";
    public static final String TWITTER = "//twitter.com";
    public static final String YAHOO = "//login.yahoo.com";

    /**
     * Attempts to translate the account type in the provided account into the string that should be used in the credentials API.
     *
     * @param account an account on the device.
     * @return The identity provider string for use with the Credentials API, or {@code null} if the account type is unknown.
     */
    public static String getIdentityProviderForAccount(Account account) {
        return null;
    }
}
