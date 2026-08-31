/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.bender3;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Bender3RedirectExtras implements Parcelable {
    public final String o1SessionId;
    public final int billableService;
    public final int regionCode;


    public Bender3RedirectExtras(Parcel source) {
        Bundle bundle = source.readBundle(this.getClass().getClassLoader());
        this.o1SessionId = bundle.getString("session_id");
        this.billableService = bundle.getInt("billable_service", -1);
        this.regionCode = bundle.getInt("region_code", -1);
    }

    public Bender3RedirectExtras(String o1SessionId, int billableService, int regionCode) {
        this.o1SessionId = o1SessionId;
        this.billableService = billableService;
        this.regionCode = regionCode;
    }

    public static final Creator<Bender3RedirectExtras> CREATOR = new Creator<Bender3RedirectExtras>() {
        @Override
        public Bender3RedirectExtras createFromParcel(Parcel in) {
            return new Bender3RedirectExtras(in);
        }

        @Override
        public Bender3RedirectExtras[] newArray(int size) {
            return new Bender3RedirectExtras[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        if (this.o1SessionId != null) {
            bundle.putString("session_id", this.o1SessionId);
        }

        if (this.billableService != -1) {
            bundle.putInt("billable_service", this.billableService);
        }

        if (this.regionCode != -1) {
            bundle.putInt("region_code", this.regionCode);
        }
        bundle.writeToParcel(dest, flags);
    }

    @NonNull
    @Override
    public String toString() {
        return "Bender3RedirectExtras{" +
                "o1SessionId=" + o1SessionId +
                ", billableService=" + this.billableService +
                ", regionCode=" + this.regionCode +
                '}';
    }
}
