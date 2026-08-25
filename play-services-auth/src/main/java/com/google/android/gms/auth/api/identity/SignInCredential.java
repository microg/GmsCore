/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import org.microg.gms.common.Hide;

import java.util.Arrays;
import java.util.Objects;

/**
 * The credential returned as a result of a successful sign-in. Data returned within this object depends on the type of
 * credential that user has selected; for example a password is returned only when a password-backed credential was
 * selected.
 */
@SafeParcelable.Class
public class SignInCredential extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getId")
    private final String id;
    @Field(value = 2, getterName = "getDisplayName")
    private final String displayName;
    @Field(value = 3, getterName = "getGivenName")
    private final String givenName;
    @Field(value = 4, getterName = "getFamilyName")
    private final String familyName;
    @Field(value = 5, getterName = "getProfilePictureUri")
    private final Uri profilePictureUri;
    @Field(value = 6, getterName = "getPassword")
    private final String password;
    @Field(value = 7, getterName = "getGoogleIdToken")
    private final String googleIdToken;
    @Field(value = 8, getterName = "getPhoneNumber")
    private final String phoneNumber;
    @Field(value = 9, getterName = "getPublicKeyCredential")
    private final PublicKeyCredential publicKeyCredential;

    @Hide
    @Constructor
    public SignInCredential(@Param(1) String id, @Param(2) String displayName, @Param(3) String givenName, @Param(4) String familyName, @Param(5) Uri profilePictureUri, @Param(6) String password, @Param(7) String googleIdToken, @Param(8) String phoneNumber, @Param(9) PublicKeyCredential publicKeyCredential) {
        this.id = id;
        this.displayName = displayName;
        this.givenName = givenName;
        this.familyName = familyName;
        this.profilePictureUri = profilePictureUri;
        this.password = password;
        this.googleIdToken = googleIdToken;
        this.phoneNumber = phoneNumber;
        this.publicKeyCredential = publicKeyCredential;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getGoogleIdToken() {
        return googleIdToken;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Returns the identifier of the credential. For an ID token credential, this returns the email address of the user's account
     * and for a password-backed credential, it returns the username for that password.
     */
    public String getId() {
        return id;
    }

    /**
     * @deprecated No replacement.
     */
    @Deprecated
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Uri getProfilePictureUri() {
        return profilePictureUri;
    }

    /**
     * Returns {@code publicKeyCredential}.
     */
    public PublicKeyCredential getPublicKeyCredential() {
        return publicKeyCredential;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignInCredential)) return false;

        SignInCredential that = (SignInCredential) o;

        if (!Objects.equals(id, that.id)) return false;
        if (!Objects.equals(displayName, that.displayName)) return false;
        if (!Objects.equals(givenName, that.givenName)) return false;
        if (!Objects.equals(familyName, that.familyName)) return false;
        if (!Objects.equals(profilePictureUri, that.profilePictureUri)) return false;
        if (!Objects.equals(password, that.password)) return false;
        if (!Objects.equals(googleIdToken, that.googleIdToken)) return false;
        if (!Objects.equals(phoneNumber, that.phoneNumber)) return false;
        return Objects.equals(publicKeyCredential, that.publicKeyCredential);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{id, displayName, givenName, familyName, profilePictureUri, password, googleIdToken, phoneNumber, publicKeyCredential});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SignInCredential> CREATOR = findCreator(SignInCredential.class);
}
