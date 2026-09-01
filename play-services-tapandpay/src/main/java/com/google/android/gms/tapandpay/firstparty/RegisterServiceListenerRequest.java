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
public class RegisterServiceListenerRequest extends AbstractSafeParcelable {
    @Field(1)
    public String s1;
    @Field(2)
    public byte[] f2;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("RegisterServiceListenerRequest").value(s1).value(f2).end();
    }

    public static final SafeParcelableCreatorAndWriter<RegisterServiceListenerRequest> CREATOR = findCreator(RegisterServiceListenerRequest.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
