/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.carrierauth;

import com.google.android.gms.common.internal.safeparcel.SafeParcelReader;
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader;
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter;

public final class EapInfoRequest extends AbstractSafeParcelable {
    public static final Parcelable.Creator<EapInfoRequest> CREATOR =
            new Parcelable.Creator<EapInfoRequest>() {
                @Override
                public EapInfoRequest createFromParcel(Parcel parcel) {
                    int end = SafeParcelReader.readObjectHeader(parcel);
                    while (parcel.dataPosition() < end) {
                        int header = SafeParcelReader.readHeader(parcel);
                        SafeParcelReader.skipUnknownField(parcel, header);
                    }
                    SafeParcelReader.ensureAtEnd(parcel, end);
                    return new EapInfoRequest();
                }

                @Override
                public EapInfoRequest[] newArray(int size) {
                    return new EapInfoRequest[size];
                }
            };

    public EapInfoRequest() {
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        int token = SafeParcelWriter.writeHeader(parcel);
        SafeParcelWriter.finishObjectHeader(parcel, token);
    }
}

