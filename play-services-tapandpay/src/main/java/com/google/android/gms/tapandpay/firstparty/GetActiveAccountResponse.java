/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.tapandpay.internal.firstparty.SetActiveAccountRequest;
import org.microg.safeparcel.AutoSafeParcelable;

@SafeParcelable.Class
public class GetActiveAccountResponse extends AbstractSafeParcelable {
    @Field(2)
    @Nullable
    public final AccountInfo accountInfo;

    @Constructor
    public GetActiveAccountResponse(@Nullable @Param(2) AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }

    public static final SafeParcelableCreatorAndWriter<GetActiveAccountResponse> CREATOR = findCreator(GetActiveAccountResponse.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
