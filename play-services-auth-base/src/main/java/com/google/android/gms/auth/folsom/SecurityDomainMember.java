/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.folsom;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class SecurityDomainMember extends AbstractSafeParcelable {

    @Field(1)
    public int memberType;
    @Field(2)
    public byte[] memberMetadata;

    public SecurityDomainMember() {
    }

    @Constructor
    public SecurityDomainMember(@Param(1) int memberType, @Param(2) byte[] memberMetadata) {
        this.memberType = memberType;
        this.memberMetadata = memberMetadata;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SecurityDomainMember> CREATOR = findCreator(SecurityDomainMember.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SecurityDomainMember")
                .field("memberType", memberType)
                .field("memberMetadata", memberMetadata != null ? memberMetadata.length : 0)
                .end();
    }
}
