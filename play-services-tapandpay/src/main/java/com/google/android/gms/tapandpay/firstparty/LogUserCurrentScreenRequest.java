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
public class LogUserCurrentScreenRequest extends AbstractSafeParcelable {
    @Field(1)
    public int i1;
    @Field(2)
    public int i2;
    @Field(3)
    public int i3;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LogUserCurrentScreenRequest").value(i1).value(i2).value(i3).end();
    }

    public static final SafeParcelableCreatorAndWriter<LogUserCurrentScreenRequest> CREATOR = findCreator(LogUserCurrentScreenRequest.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
