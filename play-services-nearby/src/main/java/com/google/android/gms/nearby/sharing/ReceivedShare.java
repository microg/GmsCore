/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.sharing;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A share that was received from a nearby device and is waiting to be consumed.
 */
public class ReceivedShare implements Parcelable {
    private final String sourceDeviceName;
    private final SharedContent sharedContent;

    public ReceivedShare(String sourceDeviceName, SharedContent sharedContent) {
        this.sourceDeviceName = sourceDeviceName;
        this.sharedContent = sharedContent;
    }

    protected ReceivedShare(Parcel in) {
        this.sourceDeviceName = in.readString();
        this.sharedContent = in.readParcelable(SharedContent.class.getClassLoader());
    }

    public String getSourceDeviceName() {
        return sourceDeviceName;
    }

    public SharedContent getSharedContent() {
        return sharedContent;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sourceDeviceName);
        dest.writeParcelable(sharedContent, flags);
    }

    public static final Creator<ReceivedShare> CREATOR = new Creator<ReceivedShare>() {
        @Override
        public ReceivedShare createFromParcel(Parcel in) {
            return new ReceivedShare(in);
        }

        @Override
        public ReceivedShare[] newArray(int size) {
            return new ReceivedShare[size];
        }
    };
}
