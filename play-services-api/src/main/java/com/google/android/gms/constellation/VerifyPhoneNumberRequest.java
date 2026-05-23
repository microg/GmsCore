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

import java.util.List;

/**
 * Request to verify a phone number via Constellation.
 *
 * - field 1: policyId - UPI policy string (e.g., "upi-carrier-id-mt-priority"), NOT phone number
 * - field 2: timeout - always 0L from Messages
 * - field 3: idTokenRequest - audience + nonce for JWT
 * - field 4: extras - Bundle with session_id, consent_type, force_provisioning, etc.
 * - field 5: imsiRequests - List of IMSI/MSISDN pairs per SIM
 * - field 6: allowFallback - whether fallback verification methods are allowed
 * - field 7: verificationType - preferred verification type
 * - field 8: verificationCapabilities - supported verification methods (List<Integer>)
 */
@SafeParcelable.Class
public class VerifyPhoneNumberRequest extends AbstractSafeParcelable {
    @Field(1)
    public String policyId;
    @Field(2)
    public long timeout;
    @Field(3)
    public IdTokenRequest idTokenRequest;
    @Field(4)
    public Bundle extras;
    @Field(5)
    public List<ImsiRequest> imsiRequests;
    @Field(6)
    public boolean allowFallback;
    @Field(7)
    public int verificationType;
    @Field(8)
    public List<Integer> verificationCapabilities;

    @Constructor
    public VerifyPhoneNumberRequest(
            @Param(1) String policyId,
            @Param(2) long timeout,
            @Param(3) IdTokenRequest idTokenRequest,
            @Param(4) Bundle extras,
            @Param(5) List<ImsiRequest> imsiRequests,
            @Param(6) boolean allowFallback,
            @Param(7) int verificationType,
            @Param(8) List<Integer> verificationCapabilities) {
        this.policyId = policyId;
        this.timeout = timeout;
        this.idTokenRequest = idTokenRequest;
        this.extras = extras;
        this.imsiRequests = imsiRequests;
        this.allowFallback = allowFallback;
        this.verificationType = verificationType;
        this.verificationCapabilities = verificationCapabilities;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerifyPhoneNumberRequest> CREATOR = findCreator(VerifyPhoneNumberRequest.class);
}
