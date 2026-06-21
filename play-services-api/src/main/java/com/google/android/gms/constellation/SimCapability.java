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

import java.util.List;

/**
 * SIM capability information for phone verification.
 *
 * Fields:
 * - subscriptionId: Android subscription ID for the SIM
 * - phoneNumber: Phone number associated with SIM (E.164)
 * - slotIndex: Physical SIM slot index
 * - carrierId: Carrier identifier string
 * - verificationCapabilities: List of supported verification methods
 */
@SafeParcelable.Class
public class SimCapability extends AbstractSafeParcelable {
    @Field(1)
    public int subscriptionId;
    @Field(2)
    public String phoneNumber;
    @Field(3)
    public int slotIndex;
    @Field(4)
    public String carrierId;
    @Field(5)
    public List<VerificationCapability> verificationCapabilities;

    @Constructor
    public SimCapability(
            @Param(1) int subscriptionId,
            @Param(2) String phoneNumber,
            @Param(3) int slotIndex,
            @Param(4) String carrierId,
            @Param(5) List<VerificationCapability> verificationCapabilities) {
        this.subscriptionId = subscriptionId;
        this.phoneNumber = phoneNumber;
        this.slotIndex = slotIndex;
        this.carrierId = carrierId;
        this.verificationCapabilities = verificationCapabilities;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SimCapability> CREATOR = findCreator(SimCapability.class);
}
