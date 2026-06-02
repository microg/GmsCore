// FILE: PhoneAuthCredential.java
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
     * Returns the unique string identifier for the phone.
     *
     * @return The unique string identifier for the phone.
     */
    public static String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the verification code for the phone.
     *
     * @param smsCode The verification code for the phone.
     */
    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }

    /**
     * Returns a boolean indicating whether the phone has a verification code.
     *
     * @return A boolean indicating whether the phone has a verification code.
     */
    public boolean hasVerificationCode() {
        return hasVerificationCode;
    }
}

// FILE: LinkPhoneAuthCredentialAidlRequest.java
/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class LinkPhoneAuthCredentialAidlRequest extends AutoSafeParcelable {
    public static final Creator<LinkPhoneAuthCredentialAidlRequest> CREATOR = new AutoCreator<>(LinkPhoneAuthCredentialAidlRequest.class);

    private String phoneNumber;
    private boolean hasVerificationCode;
    private String temporaryProof;
    private String mfaEnrollmentId;

    public LinkPhoneAuthCredentialAidlRequest(String phoneNumber, boolean hasVerificationCode,
                                             String temporaryProof, String mfaEnrollmentId) {
        this.phoneNumber = phoneNumber;
        this.hasVerificationCode = hasVerificationCode;
        this.temporaryProof = temporaryProof;
        this.mfaEnrollmentId = mfaEnrollmentId;
    }

    public static class Builder {
        private String phoneNumber;
        private boolean hasVerificationCode;
        private String temporaryProof;
        private String mfaEnrollmentId;

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder setHasVerificationCode(boolean hasVerificationCode) {
            this.hasVerificationCode = hasVerificationCode;
            return this;
        }

        public Builder setTemporaryProof(String temporaryProof) {
            this.temporaryProof = temporaryProof;
            return this;
        }

        public Builder setMfaEnrollmentId(String mfaEnrollmentId) {
            this.mfaEnrollmentId = mfaEnrollmentId;
            return this;
        }

        public LinkPhoneAuthCredentialAidlRequest build() {
            return new LinkPhoneAuthCredentialAidlRequest(phoneNumber, hasVerificationCode,
                    temporaryProof, mfaEnrollmentId);
        }
    }

    // Other methods...
}

// FILE: SignInWithPhoneNumberAidlRequest.java
/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import com.google.firebase.auth.PhoneAuthCredential;

import org.microg.safeparcel.AutoSafeParcelable;

public class SignInWithPhoneNumberAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public LinkPhoneAuthCredentialAidlRequest credential;
    @Field(2)
    public String tenantId;

    public static final Creator<SignInWithPhoneNumberAidlRequest> CREATOR = new AutoCreator<>(SignInWithPhoneNumberAidlRequest.class);

    // Other methods...
}

// FILE: StartMfaPhoneNumberEnrollmentAidlRequest.java
/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class StartMfaPhoneNumberEnrollmentAidlRequest extends AutoSafeParcelable {
    public static final Creator<StartMfaPhoneNumberEnrollmentAidlRequest> CREATOR = new AutoCreator<>(StartMfaPhoneNumberEnrollmentAidlRequest.class);

    // Other methods...
}

// FILE: .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "daily"
    open-pull-requests-limit: 1