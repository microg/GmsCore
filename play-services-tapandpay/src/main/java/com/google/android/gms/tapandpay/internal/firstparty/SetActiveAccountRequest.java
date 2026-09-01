/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.internal.firstparty;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class SetActiveAccountRequest extends AbstractSafeParcelable {
    @Field(2)
    public String accountName;
    @Field(3)
    public boolean allowSetupErrorMessage;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SetActiveAccountRequest").value(accountName)
                .field("allowSetupErrorMessage", allowSetupErrorMessage)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<SetActiveAccountRequest> CREATOR = findCreator(SetActiveAccountRequest.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
