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
public class SetSelectedTokenRequest extends AbstractSafeParcelable {
    @Field(2)
    public String clientTokenId;
    @Field(3)
    public long time;
    @Field(4)
    public boolean b4;
    @Field(5)
    public int priority;
    @Field(6)
    public long l6;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SetSelectedTokenRequest")
                .field("clientTokenId", clientTokenId)
                .field("time", time)
                .field("b4", b4)
                .field("priority", priority)
                .field("l6", l6)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<SetSelectedTokenRequest> CREATOR = findCreator(SetSelectedTokenRequest.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
