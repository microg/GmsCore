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
public class OnlineAccountCardLinkInfo extends AbstractSafeParcelable {
    @Field(1)
    public int accountType;
    @Field(2)
    public int status;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("OnlineAccountCardLinkInfo")
                .field("accountType", accountType)
                .field("status", status)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<OnlineAccountCardLinkInfo> CREATOR = findCreator(OnlineAccountCardLinkInfo.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
