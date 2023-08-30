/*
 * SPDX-FileCopyrightText: 2012, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.location;

import android.location.LocationRequest;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * @hide
 */
public final class ProviderRequest implements Parcelable {
    /**
     * Location reporting is requested (true)
     */
    public boolean reportLocation = false;

    /**
     * The smallest requested interval
     */
    public long interval = Long.MAX_VALUE;

    /**
     * A more detailed set of requests.
     * <p>Location Providers can optionally use this to
     * fine tune location updates, for example when there
     * is a high power slow interval request and a
     * low power fast interval request.
     */
    public List<LocationRequest> locationRequests = null;

    public ProviderRequest() {
    }

    public static final Parcelable.Creator<ProviderRequest> CREATOR =
            new Parcelable.Creator<ProviderRequest>() {
                @Override
                public ProviderRequest createFromParcel(Parcel in) {
                    return null;
                }

                @Override
                public ProviderRequest[] newArray(int size) {
                    return null;
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
    }
}
