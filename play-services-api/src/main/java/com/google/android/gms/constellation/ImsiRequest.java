/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Request containing IMSI and associated metadata.
 *
 * Fields:
 * - imsi: IMSI string
 * - msisdn: MSISDN/phone number (E.164 string)
 */
@SafeParcelable.Class
public class ImsiRequest extends AbstractSafeParcelable {
    @Field(1)
    public String imsi;
    @Field(2)
    public String msisdn;

    @Constructor
    public ImsiRequest(@Param(1) String imsi, @Param(2) String msisdn) {
        this.imsi = imsi;
        this.msisdn = msisdn;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ImsiRequest> CREATOR = findCreator(ImsiRequest.class);
}
