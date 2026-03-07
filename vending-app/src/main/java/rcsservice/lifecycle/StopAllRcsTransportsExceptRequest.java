/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcsservice.lifecycle;

import android.os.Parcel;
import android.os.Parcelable;

public class StopAllRcsTransportsExceptRequest implements Parcelable {
    private int subId;

    public StopAllRcsTransportsExceptRequest(int subId) {
        this.subId = subId;
    }

    protected StopAllRcsTransportsExceptRequest(Parcel in) {
        this.subId = in.readInt();
    }

    public int getSubId() {
        return subId;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(subId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StopAllRcsTransportsExceptRequest> CREATOR = new Creator<StopAllRcsTransportsExceptRequest>() {
        @Override
        public StopAllRcsTransportsExceptRequest createFromParcel(Parcel in) {
            return new StopAllRcsTransportsExceptRequest(in);
        }

        @Override
        public StopAllRcsTransportsExceptRequest[] newArray(int size) {
            return new StopAllRcsTransportsExceptRequest[size];
        }
    };
}
