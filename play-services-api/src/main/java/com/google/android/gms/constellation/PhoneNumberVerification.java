/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Represents a phone number verification result from Constellation.
 * 
 * Fields: phoneNumber, timestamp, verificationMethod, errorCode, token, extras,
 * verificationStatus, retryAfterSeconds.
 *
 * AIDL verificationStatus values differ from proto VerificationState:
 * Proto: UNKNOWN=0, NONE=1, PENDING=2, VERIFIED=3
 * AIDL: 1=VERIFIED
 */
@SafeParcelable.Class
public class PhoneNumberVerification extends AbstractSafeParcelable {
    @Field(1)
    public String phoneNumber;
    @Field(2)
    public long timestamp;
    @Field(3)
    public int verificationMethod;  // Was incorrectly named verificationState
    @Field(4)
    public int errorCode;           // Was incorrectly at field 7
    @Field(5)
    public String token;
    @Field(6)
    public Bundle extras;
    @Field(7)
    public int verificationStatus;  // Was incorrectly named errorCode
    @Field(8)
    public long retryAfterSeconds;

    @Constructor
    public PhoneNumberVerification(
            @Param(1) String phoneNumber,
            @Param(2) long timestamp,
            @Param(3) int verificationMethod,
            @Param(4) int errorCode,
            @Param(5) String token,
            @Param(6) Bundle extras,
            @Param(7) int verificationStatus,
            @Param(8) long retryAfterSeconds) {
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.verificationMethod = verificationMethod;
        this.errorCode = errorCode;
        this.token = token;
        this.extras = extras;
        this.verificationStatus = verificationStatus;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PhoneNumberVerification> CREATOR = findCreator(PhoneNumberVerification.class);

    // Verification status (field 7) - AIDL values (NOT proto values!)
    // Evidence: bazj.java:287+314, bazk.java:296+387 all set g=1 for verified numbers
    // bayl.java:22-28 requires phone number when g==1 ("Invalid verifiedPhoneNumber")
    public static final int STATUS_VERIFIED = 1;  // AIDL verified (proto would be 3)
    public static final int STATUS_UNVERIFIED = 0; // NOT_VERIFIED
    public static final int STATUS_SMS_VERIFICATION_FAILED = 4;
    // Messages uses status 7 as a non-retryable failure that triggers manual MSISDN fallback
    public static final int STATUS_NON_RETRYABLE_FAILURE = 7;
    // Messages uses status 8 to mark UPI ineligible and still sends a token
    public static final int STATUS_INELIGIBLE = 8;
    // Other status values are less certain, but validation allows 0-10

    // Verification method (field 3) - AIDL values differ from proto enum values
    public static final int METHOD_UNKNOWN = 0;
    public static final int METHOD_MO_SMS = 1;
    public static final int METHOD_MT_SMS = 2;
    public static final int METHOD_CARRIER_ID = 3;
    public static final int METHOD_IMSI_LOOKUP = 5;
    public static final int METHOD_REGISTERED_SMS = 7;
    public static final int METHOD_FLASH_CALL = 8;
    public static final int METHOD_TS43_AIDL = 9;     // TS43 in AIDL (proto gbqb.TS43 = 11, but AIDL uses 9)
    public static final int METHOD_TS43 = 11;         // gbqb.TS43 proto value - DO NOT use in PhoneNumberVerification

    // Error code (field 4) - defaults to -1, valid range: >= 0 or == -1
    public static final int ERROR_NONE = -1;
}
