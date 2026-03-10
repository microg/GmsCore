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
public class InStoreCvmConfig extends AbstractSafeParcelable {
    @Field(2)
    public boolean requireCdcvmPassing;
    @Field(3)
    public int cdcvmExpirationInSecs;
    @Field(4)
    public int unlockedTapLimit;
    @Field(5)
    public int cdcvmTapLimit;
    @Field(6)
    public boolean prioritizeOnlinePinOverCdcvm;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("InStoreCvmConfig")
                .field("requireCdcvmPassing", requireCdcvmPassing)
                .field("cdcvmExpirationInSecs", cdcvmExpirationInSecs)
                .field("unlockedTapLimit", unlockedTapLimit)
                .field("cdcvmTapLimit", cdcvmTapLimit)
                .field("prioritizeOnlinePinOverCdcvm", prioritizeOnlinePinOverCdcvm)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<InStoreCvmConfig> CREATOR = findCreator(InStoreCvmConfig.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
