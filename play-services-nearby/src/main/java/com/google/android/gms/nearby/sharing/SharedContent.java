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
 * The payload that is transferred to a {@link ShareTarget}.
 */
public class SharedContent implements Parcelable {
    private final String text;

    public SharedContent(String text) {
        this.text = text;
    }

    protected SharedContent(Parcel in) {
        this.text = in.readString();
    }

    /**
     * The text attached to this share, or {@code null} if this share carries no text.
     */
    public String getText() {
        return text;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
    }

    public static final Creator<SharedContent> CREATOR = new Creator<SharedContent>() {
        @Override
        public SharedContent createFromParcel(Parcel in) {
            return new SharedContent(in);
        }

        @Override
        public SharedContent[] newArray(int size) {
            return new SharedContent[size];
        }
    };
}
