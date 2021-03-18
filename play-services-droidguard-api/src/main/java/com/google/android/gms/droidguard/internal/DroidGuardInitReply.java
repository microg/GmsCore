/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard.internal;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

public class DroidGuardInitReply implements Parcelable {
    public ParcelFileDescriptor pfd;
    public Parcelable object;

    public DroidGuardInitReply(ParcelFileDescriptor pfd, Parcelable object) {
        this.pfd = pfd;
        this.object = object;
    }

    @Override
    public int describeContents() {
        return (pfd != null ? Parcelable.CONTENTS_FILE_DESCRIPTOR : 0) | (object != null ? object.describeContents() : 0);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(pfd, flags);
        dest.writeParcelable(object, flags);
    }

    public final static Creator<DroidGuardInitReply> CREATOR = new Creator<DroidGuardInitReply>() {
        @Override
        public DroidGuardInitReply createFromParcel(Parcel source) {
            ParcelFileDescriptor pfd = source.readParcelable(ParcelFileDescriptor.class.getClassLoader());
            Parcelable object = source.readParcelable(getClass().getClassLoader());
            if (pfd != null && object != null) {
                return new DroidGuardInitReply(pfd, object);
            }
            return null;
        }

        @Override
        public DroidGuardInitReply[] newArray(int size) {
            return new DroidGuardInitReply[size];
        }
    };
}
