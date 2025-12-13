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
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class BadgeInfo extends AbstractSafeParcelable {
    @Field(1)
    public String clientTokenId;
    @Field(2)
    public byte[] serverToken;
    @Field(3)
    public int cardNetwork;
    @Field(4)
    public TokenStatus tokenStatus;
    @Field(5)
    public String tokenLastDigits;
    @Field(6)
    public TransactionInfo transactionInfo;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("BadgeInfo")
                .field("clientTokenId", clientTokenId)
                .field("serverToken", serverToken)
                .field("cardNetwork", cardNetwork)
                .field("tokenStatus", tokenStatus)
                .field("tokenLastDigits", tokenLastDigits)
                .field("transactionInfo", transactionInfo)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<BadgeInfo> CREATOR = findCreator(BadgeInfo.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
