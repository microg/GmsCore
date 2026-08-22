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

public final class EAPAKARequest extends AbstractSafeParcelable {
    public static final Parcelable.Creator<EAPAKARequest> CREATOR =
            new Parcelable.Creator<EAPAKARequest>() {
                @Override
                public EAPAKARequest createFromParcel(Parcel parcel) {
                    String a = null;
                    Integer b = null;
                    Integer c = null;
                    Integer d = null;
                    int e = 0;
                    int end = SafeParcelReader.readObjectHeader(parcel);
                    while (parcel.dataPosition() < end) {
                        int header = SafeParcelReader.readHeader(parcel);
                        switch (SafeParcelReader.getFieldId(header)) {
                            case 1:
                                a = SafeParcelReader.createString(parcel, header);
                                break;
                            case 2:
                                b = SafeParcelReader.readIntegerObject(parcel, header);
                                break;
                            case 3:
                                c = SafeParcelReader.readIntegerObject(parcel, header);
                                break;
                            case 4:
                                d = SafeParcelReader.readIntegerObject(parcel, header);
                                break;
                            case 5:
                                e = SafeParcelReader.readInt(parcel, header);
                                break;
                            default:
                                SafeParcelReader.skipUnknownField(parcel, header);
                        }
                    }
                    SafeParcelReader.ensureAtEnd(parcel, end);
                    return new EAPAKARequest(a, b, c, d, e);
                }

                @Override
                public EAPAKARequest[] newArray(int size) {
                    return new EAPAKARequest[size];
                }
            };

    public final String a;
    public final Integer b;
    public final Integer c;
    public final Integer d;
    public final int e;

    public EAPAKARequest(String a, Integer b, Integer c, Integer d, int e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        int token = SafeParcelWriter.writeHeader(parcel);
        SafeParcelWriter.writeString(parcel, 1, a, false);
        SafeParcelWriter.writeIntegerObject(parcel, 2, b, false);
        SafeParcelWriter.writeIntegerObject(parcel, 3, c, false);
        SafeParcelWriter.writeIntegerObject(parcel, 4, d, false);
        SafeParcelWriter.writeInt(parcel, 5, e);
        SafeParcelWriter.finishObjectHeader(parcel, token);
    }
}

