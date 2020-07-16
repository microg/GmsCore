/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    private String id;

    @SafeParceled(2)
    private String name;

    @SafeParceled(3)
    private Uri profilePictureUri;

    @SafeParceled(value = 4, subClass = IdToken.class)
    private List<IdToken> tokens;

    @SafeParceled(5)
    private String password;

    @SafeParceled(6)
    private String accountType;

    @SafeParceled(7)
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
        if (o == null || !(o instanceof Credential)) return false;

        Credential that = (Credential) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (profilePictureUri != null ? !profilePictureUri.equals(that.profilePictureUri) : that.profilePictureUri != null)
            return false;
        if (password != null ? !password.equals(that.password) : that.password != null)
            return false;
        if (accountType != null ? !accountType.equals(that.accountType) : that.accountType != null)
            return false;
        return generatedPassword != null ? generatedPassword.equals(that.generatedPassword) : that.generatedPassword == null;

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{id, name, profilePictureUri, password, accountType, generatedPassword});
    }

    public static class Builder {
        private String id;
        private String name;
        private Uri profilePictureUri;
        private String password;
        private String accountType;

        @PublicApi(exclude = true)
        public List<IdToken> tokens;
        @PublicApi(exclude = true)
        private String generatedPassword;

        public Builder(String id) {
            this.id = id;
        }

        /**
         * Copies the information stored in an existing credential, in order to allow that information to be modified.
         *
         * @param credential the existing credential
         */
        public Builder(Credential credential) {
            this.id = credential.id;
            this.name = credential.name;
            this.profilePictureUri = credential.profilePictureUri;
            this.password = credential.password;
            this.accountType = credential.accountType;
            this.tokens = credential.tokens;
            this.generatedPassword = credential.generatedPassword;
        }

        public Credential build() {
            Credential credential = new Credential();
            credential.id = id;
            credential.name = name;
            credential.profilePictureUri = profilePictureUri;
            credential.password = password;
            credential.accountType = accountType;
            credential.tokens = tokens;
            credential.generatedPassword = generatedPassword;
            return credential;
        }

        /**
         * Specifies the account type for a federated credential. The value should be set to
         * identity provider's login domain, such as "https://accounts.google.com" for Google
         * accounts. The login domains for common identity providers are listed in {@link IdentityProviders}.
         *
         * @param accountType The type of the account. Typically, one of the values in {@link IdentityProviders}.
         */
        public Builder setAccountType(String accountType) {
            this.accountType = accountType;
            return this;
        }

        /**
         * Sets the display name for the credential, which should be easy for the user to recognize
         * as associated to the credential, and distinguishable from other credentials they may
         * have. This string will be displayed more prominently than, or instead of, the account ID
         * whenever available. In most cases, the name of the user is sufficient.
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the password for the credential. Either the password or the account type must be
         * set for a credential, but not both.
         */
        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets a profile picture associated with the credential, typically a picture the user has
         * selected to represent the account.
         */
        public Builder setProfilePictureUri(Uri profilePictureUri) {
            this.profilePictureUri = profilePictureUri;
            return this;
        }
    }

    public static final Creator<Credential> CREATOR = new AutoCreator<Credential>(Credential.class);
}
