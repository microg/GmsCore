/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

@PublicApi
@SafeParcelable.Class
public class GoogleThirdPartyPaymentExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "isThirdPartyPayment")
    private final boolean thirdPartyPayment;

    @Constructor
    public GoogleThirdPartyPaymentExtension(@Param(1) boolean thirdPartyPayment) {
        this.thirdPartyPayment = thirdPartyPayment;
    }

    public boolean isThirdPartyPayment() {
        return thirdPartyPayment;
    }

    @Override
    public String toString() {
        return ToStringHelper.name("GoogleThirdPartyPaymentExtension").field("thirdPartyPayment", thirdPartyPayment).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleThirdPartyPaymentExtension> CREATOR = AbstractSafeParcelable.findCreator(GoogleThirdPartyPaymentExtension.class);
}
