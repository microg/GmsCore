/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.credentials;

import android.net.Uri;
import android.text.TextUtils;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@PublicApi
public class Credential extends AutoSafeParcelable {

    @Field(1000)
    private int versionCode = 1;

    @Field(1)
    private String id;
    @Field(2)
    private String name;
    @Field(3)
    private Uri profilePictureUri;
    @Field(value = 4, subClass = IdToken.class)
    private List<IdToken> tokens;
    @Field(5)
    private String password;
    @Field(6)
    private String accountType;
    @Field(7)
    private String generatedPassword;

    private Credential() {
    }

    /**
     * Returns the type of federated identity account used to sign in the user. While this may be
     * any string, it is strongly recommended that values from {@link com.google.android.gms.auth.api.credentials.IdentityProviders}
     * are used, which are the login domains for common identity providers.
     *
     * @return A string identifying the federated identity provider associated with this account,
     * typically in the form of the identity provider's login domain. null will be returned if the
     * credential is a password credential.
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * Returns the generated password for an account hint.
     */
    public String getGeneratedPassword() {
        return generatedPassword;
    }

    /**
     * Returns the credential identifier, typically an email address or user name, though it may
     * also be some encoded unique identifier for a federated identity account.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the ID tokens that assert the identity of the user, if available. ID tokens provide
     * a secure mechanism to verify that the user owns the identity asserted by the credential.
     * <p/>
     * This is useful for account hints, where the ID token can replace the need to separately
     * verify that the user owns their claimed email address - with a valid ID token, it is not
     * necessary to send an account activation link to the address, simplifying the account
     * creation process for the user.
     * <p/>
     * A signed ID token is returned automatically for credential hints when the credential ID is a
     * Google account that is authenticated on the device. This ID token can be sent along with
     * your application's account creation operation, where the signature can be verified.
     */
    public List<IdToken> getIdTokens() {
        return tokens;
    }

    /**
     * Returns the display name of the credential, if available. Typically, the display name will
     * be the name of the user, or some other string which the user can easily recognize and
     * distinguish from other accounts they may have.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the password used to sign in the user.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the URL to an image of the user, if available.
     */
    public Uri getProfilePictureUri() {
        return profilePictureUri;
    }

    @PublicApi(exclude = true)
    public String getAsString() {
        if (TextUtils.isEmpty(accountType)) {
            return id.toLowerCase(Locale.US) + "|";
        } else {
            Uri uri = Uri.parse(accountType);
            return id.toLowerCase(Locale.US) + "|" + (TextUtils.isEmpty(uri.getScheme()) ? "" : uri.getScheme().toLowerCase(Locale.US)) + "://" +
                    (TextUtils.isEmpty(uri.getHost()) ? "unknown" : uri.getHost().toLowerCase(Locale.US)) + ":" + uri.getPort();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Credential that = (Credential) o;

        if (versionCode != that.versionCode) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (profilePictureUri != null ? !profilePictureUri.equals(that.profilePictureUri) : that.profilePictureUri != null)
            return false;
        if (tokens != null ? !tokens.equals(that.tokens) : that.tokens != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (accountType != null ? !accountType.equals(that.accountType) : that.accountType != null) return false;
        return generatedPassword != null ? generatedPassword.equals(that.generatedPassword) : that.generatedPassword == null;

    }

    @Override
    public int hashCode() {
        int result = versionCode;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (profilePictureUri != null ? profilePictureUri.hashCode() : 0);
        result = 31 * result + (tokens != null ? tokens.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (accountType != null ? accountType.hashCode() : 0);
        result = 31 * result + (generatedPassword != null ? generatedPassword.hashCode() : 0);
        return result;
    }

    public static final Creator<Credential> CREATOR = new AutoCreator<Credential>(Credential.class);
}