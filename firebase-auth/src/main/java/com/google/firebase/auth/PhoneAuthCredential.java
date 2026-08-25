/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.firebase.auth;

import org.microg.gms.common.PublicApi;

/**
 * Wraps phone number and verification information for authentication purposes.
 */
@PublicApi
public class PhoneAuthCredential extends AuthCredential {
    @Field(1)
    @PublicApi(exclude = true)
    public String sessionInfo;
    @Field(2)
    @PublicApi(exclude = true)
    public String smsCode;
    @Field(3)
    @PublicApi(exclude = true)
    public boolean hasVerificationCode;
    @Field(4)
    @PublicApi(exclude = true)
    public String phoneNumber;
    @Field(5)
    @PublicApi(exclude = true)
    public boolean autoCreate;
    @Field(6)
    @PublicApi(exclude = true)
    public String temporaryProof;
    @Field(7)
    @PublicApi(exclude = true)
    public String mfaEnrollmentId;

    /**
     * Returns the unique string identifier for the provider type with which the credential is associated.
     */
    @Override
    public String getProvider() {
        return "phone";
    }

    /**
     * Returns the unique string identifier for the sign in method with which the credential is associated. Should match that returned by {@link FirebaseAuth#fetchSignInMethodsForEmail(String)} after this user has signed in with this type of credential.
     */
    @Override
    public String getSignInMethod() {
        return "phone";
    }

    /**
     * Gets the auto-retrieved SMS verification code if applicable. When SMS verification is used, you will be called back first via onCodeSent(String, PhoneAuthProvider.ForceResendingToken), and later onVerificationCompleted(PhoneAuthCredential) with a {@link PhoneAuthCredential} containing a non-null SMS code if auto-retrieval succeeded. If Firebase used another approach to verify the phone number and triggers a callback via onVerificationCompleted(PhoneAuthCredential), then SMS code can be null.
     */
    public String getSmsCode() {
        return smsCode;
    }

    public static final Creator<PhoneAuthCredential> CREATOR = new AutoCreator<>(PhoneAuthCredential.class);
}
