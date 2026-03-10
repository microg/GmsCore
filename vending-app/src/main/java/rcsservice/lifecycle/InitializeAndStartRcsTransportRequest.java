/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcsservice.lifecycle;

import android.os.Parcel;
import android.os.Parcelable;

public class InitializeAndStartRcsTransportRequest implements Parcelable {
    private int subId;
    private int flags;

    public InitializeAndStartRcsTransportRequest(int subId, int flags) {
        this.subId = subId;
        this.flags = flags;
    }

    protected InitializeAndStartRcsTransportRequest(Parcel in) {
        this.subId = in.readInt();
        this.flags = in.readInt();
    }

    public int getSubId() {
        return subId;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(subId);
        dest.writeInt(this.flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<InitializeAndStartRcsTransportRequest> CREATOR = new Creator<InitializeAndStartRcsTransportRequest>() {
        @Override
        public InitializeAndStartRcsTransportRequest createFromParcel(Parcel in) {
            return new InitializeAndStartRcsTransportRequest(in);
        }

        @Override
        public InitializeAndStartRcsTransportRequest[] newArray(int size) {
            return new InitializeAndStartRcsTransportRequest[size];
        }
    };
}
