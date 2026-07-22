/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetSecurityParamsResponse extends AbstractSafeParcelable {
    @Field(2)
    public final boolean isDeviceSecure;
    @Field(3)
    public final boolean hasValidPaymentBundles;
    @Field(4)
    public final boolean hasDigitalCarKey;
    @Field(5)
    public final boolean hasMobileDocument;

    @Constructor
    public GetSecurityParamsResponse(@Param(2) boolean isDeviceSecure, @Param(3) boolean hasValidPaymentBundles, @Param(4) boolean hasDigitalCarKey, @Param(5) boolean hasMobileDocument) {
        this.isDeviceSecure = isDeviceSecure;
        this.hasValidPaymentBundles = hasValidPaymentBundles;
        this.hasDigitalCarKey = hasDigitalCarKey;
        this.hasMobileDocument = hasMobileDocument;
    }

    public static final SafeParcelableCreatorAndWriter<GetSecurityParamsResponse> CREATOR = findCreator(GetSecurityParamsResponse.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
