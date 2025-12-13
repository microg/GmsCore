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
public class TransactionInfo extends AbstractSafeParcelable {
    @Field(2)
    public int transactionDelivery;
    @Field(3)
    public int transactionLimit;
    @Field(4)
    public int supportedTransactions;
    @Field(5)
    public int deliveryPreference;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TransactionInfo")
                .field("transactionDelivery", transactionDelivery)
                .field("transactionLimit", transactionLimit)
                .field("supportedTransactions", supportedTransactions)
                .field("deliveryPreference", deliveryPreference)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<TransactionInfo> CREATOR = findCreator(TransactionInfo.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
