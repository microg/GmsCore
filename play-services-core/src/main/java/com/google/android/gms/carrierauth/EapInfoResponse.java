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

/** Structural response contract; fields must be extended when the real binary schema is known. */
public final class EapInfoResponse extends AbstractSafeParcelable {
    public static final Parcelable.Creator<EapInfoResponse> CREATOR =
            new Parcelable.Creator<EapInfoResponse>() {
                @Override
                public EapInfoResponse createFromParcel(Parcel parcel) {
                    int end = SafeParcelReader.readObjectHeader(parcel);
                    while (parcel.dataPosition() < end) {
                        SafeParcelReader.skipUnknownField(parcel, SafeParcelReader.readHeader(parcel));
                    }
                    SafeParcelReader.ensureAtEnd(parcel, end);
                    return new EapInfoResponse();
                }

                @Override
                public EapInfoResponse[] newArray(int size) {
                    return new EapInfoResponse[size];
                }
            };

    public EapInfoResponse() {
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        int token = SafeParcelWriter.writeHeader(parcel);
        SafeParcelWriter.finishObjectHeader(parcel, token);
    }
}

