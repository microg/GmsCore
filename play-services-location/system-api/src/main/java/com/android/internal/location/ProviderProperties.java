/*
 * SPDX-FileCopyrightText: 2021 The Android Open Source Project
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.location;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Parcelable containing (legacy) location provider properties.
 * This object is just used inside the framework and system services.
 *
 * @hide
 */
public final class ProviderProperties implements Parcelable {
    /**
     * True if provider requires access to a
     * data network (e.g., the Internet), false otherwise.
     */
    public final boolean mRequiresNetwork;

    /**
     * True if the provider requires access to a
     * satellite-based positioning system (e.g., GPS), false
     * otherwise.
     */
    public final boolean mRequiresSatellite;

    /**
     * True if the provider requires access to an appropriate
     * cellular network (e.g., to make use of cell tower IDs), false
     * otherwise.
     */
    public final boolean mRequiresCell;

    /**
     * True if the use of this provider may result in a
     * monetary charge to the user, false if use is free.  It is up to
     * each provider to give accurate information. Cell (network) usage
     * is not considered monetary cost.
     */
    public final boolean mHasMonetaryCost;

    /**
     * True if the provider is able to provide altitude
     * information, false otherwise.  A provider that reports altitude
     * under most circumstances but may occasionally not report it
     * should return true.
     */
    public final boolean mSupportsAltitude;

    /**
     * True if the provider is able to provide speed
     * information, false otherwise.  A provider that reports speed
     * under most circumstances but may occasionally not report it
     * should return true.
     */
    public final boolean mSupportsSpeed;

    /**
     * True if the provider is able to provide bearing
     * information, false otherwise.  A provider that reports bearing
     * under most circumstances but may occasionally not report it
     * should return true.
     */
    public final boolean mSupportsBearing;

    /**
     * Power requirement for this provider.
     *
     * @return the power requirement for this provider, as one of the
     * constants Criteria.POWER_*.
     */
    public final int mPowerRequirement;

    /**
     * Constant describing the horizontal accuracy returned
     * by this provider.
     *
     * @return the horizontal accuracy for this provider, as one of the
     * constants Criteria.ACCURACY_COARSE or Criteria.ACCURACY_FINE
     */
    public final int mAccuracy;

    public ProviderProperties(boolean mRequiresNetwork,
            boolean mRequiresSatellite, boolean mRequiresCell, boolean mHasMonetaryCost,
            boolean mSupportsAltitude, boolean mSupportsSpeed, boolean mSupportsBearing,
            int mPowerRequirement, int mAccuracy) {
        this.mRequiresNetwork = mRequiresNetwork;
        this.mRequiresSatellite = mRequiresSatellite;
        this.mRequiresCell = mRequiresCell;
        this.mHasMonetaryCost = mHasMonetaryCost;
        this.mSupportsAltitude = mSupportsAltitude;
        this.mSupportsSpeed = mSupportsSpeed;
        this.mSupportsBearing = mSupportsBearing;
        this.mPowerRequirement = mPowerRequirement;
        this.mAccuracy = mAccuracy;
    }

    public static final Parcelable.Creator<ProviderProperties> CREATOR =
            new Parcelable.Creator<ProviderProperties>() {
                @Override
                public ProviderProperties createFromParcel(Parcel in) {
                    return null;
                }

                @Override
                public ProviderProperties[] newArray(int size) {
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
