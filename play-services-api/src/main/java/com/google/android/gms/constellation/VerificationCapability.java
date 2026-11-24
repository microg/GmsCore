/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Represents a verification capability/method.
 *
 * Fields:
 * - verificationType: Type of verification (SMS OTP, EAP-AKA, etc.)
 * - priority: Priority order for this verification method
 *
 * Verification types (inferred from constellation.proto):
 * - 0: Unknown
 * - 1: SMS OTP
 * - 2: EAP-AKA (TS.43)
 * - 3: Silent verification
 * - 4: Device attestation
 */
@SafeParcelable.Class
public class VerificationCapability extends AbstractSafeParcelable {
    @Field(1)
    public int verificationType;
    @Field(2)
    public int priority;

    @Constructor
    public VerificationCapability(
            @Param(1) int verificationType,
            @Param(2) int priority) {
        this.verificationType = verificationType;
        this.priority = priority;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerificationCapability> CREATOR = findCreator(VerificationCapability.class);

    // Verification types (inferred)
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_SMS_OTP = 1;
    public static final int TYPE_EAP_AKA = 2;  // TS.43
    public static final int TYPE_SILENT = 3;
    public static final int TYPE_ATTESTATION = 4;
}
