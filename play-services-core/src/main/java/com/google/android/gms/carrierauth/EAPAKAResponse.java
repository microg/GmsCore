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

public final class EAPAKAResponse extends AbstractSafeParcelable {
    public static final Parcelable.Creator<EAPAKAResponse> CREATOR =
            new Parcelable.Creator<EAPAKAResponse>() {
                @Override
                public EAPAKAResponse createFromParcel(Parcel parcel) {
                    String a = null;
                    int end = SafeParcelReader.readObjectHeader(parcel);
                    while (parcel.dataPosition() < end) {
                        int header = SafeParcelReader.readHeader(parcel);
                        if (SafeParcelReader.getFieldId(header) == 1) {
                            a = SafeParcelReader.createString(parcel, header);
                        } else {
                            SafeParcelReader.skipUnknownField(parcel, header);
                        }
                    }
                    SafeParcelReader.ensureAtEnd(parcel, end);
                    return new EAPAKAResponse(a);
                }

                @Override
                public EAPAKAResponse[] newArray(int size) {
                    return new EAPAKAResponse[size];
                }
            };

    public final String a;

    public EAPAKAResponse(String a) {
        this.a = a;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        int token = SafeParcelWriter.writeHeader(parcel);
        SafeParcelWriter.writeString(parcel, 1, a, false);
        SafeParcelWriter.finishObjectHeader(parcel, token);
    }
}

