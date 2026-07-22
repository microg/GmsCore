/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.gcm;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class PendingCallback implements Parcelable {
    private final IBinder binder;

    public PendingCallback(IBinder binder) {
        this.binder = binder;
    }

    private PendingCallback(Parcel in) {
        this.binder = in.readStrongBinder();
    }

    public IBinder getBinder() {
        return binder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(binder);
    }

    public static final Creator<PendingCallback> CREATOR = new Creator<PendingCallback>() {
        @Override
        public PendingCallback createFromParcel(Parcel source) {
            return new PendingCallback(source);
        }

        @Override
        public PendingCallback[] newArray(int size) {
            return new PendingCallback[size];
        }
    };
}
