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
public class InAppCvmConfig extends AbstractSafeParcelable {
    @Field(2)
    public int cdcvmExpirationInSecs;
    @Field(3)
    public int cdcvmTransactionLimit;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("InAppCvmConfig")
                .field("cdcvmExpirationInSecs", cdcvmExpirationInSecs)
                .field("cdcvmTransactionLimit", cdcvmTransactionLimit)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<InAppCvmConfig> CREATOR = findCreator(InAppCvmConfig.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
