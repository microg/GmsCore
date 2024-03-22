/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Specifies the types of location services the client is interested in using. Settings will be checked for optimal functionality
 * of all requested services. Use {@link LocationSettingsRequest.Builder} to construct this object.
 */
@PublicApi
@SafeParcelable.Class
public class LocationSettingsRequest extends AbstractSafeParcelable {
    @Field(1000)
    private int versionCode = 2;

    @Field(value = 1, subClass = LocationRequest.class)
    @Hide
    public final List<LocationRequest> requests;

    @Field(2)
    @Hide
    public final boolean alwaysShow;

    @Field(3)
    @Hide
    public final boolean needBle;

    @Field(5)
    @Hide
    @Nullable
    public final LocationSettingsConfiguration configuration;

    @Constructor
    LocationSettingsRequest(@Param(1) List<LocationRequest> requests, @Param(2) boolean alwaysShow, @Param(3) boolean needBle, @Param(5) @Nullable LocationSettingsConfiguration configuration) {
        this.requests = requests;
        this.alwaysShow = alwaysShow;
        this.needBle = needBle;
        this.configuration = configuration;
    }

    /**
     * A builder that builds {@link LocationSettingsRequest}.
     */
    public static class Builder {
        private List<LocationRequest> requests = new ArrayList<LocationRequest>();
        private boolean alwaysShow = false;
        private boolean needBle = false;

        /**
         * Adds a collection of {@link LocationRequest}s that the client is interested in. Settings
         * will be checked for optimal performance of all {@link LocationRequest}s.
         */
        public Builder addAllLocationRequests(Collection<LocationRequest> requests) {
            this.requests.addAll(requests);
            return this;
        }

        /**
         * Adds one {@link LocationRequest} that the client is interested in. Settings will be
         * checked for optimal performance of all {@link LocationRequest}s.
         */
        public Builder addLocationRequest(LocationRequest request) {
            requests.add(request);
            return this;
        }

        /**
         * Creates a LocationSettingsRequest that can be used with SettingsApi.
         */
        public LocationSettingsRequest build() {
            return new LocationSettingsRequest(requests, alwaysShow, needBle, null);
        }

        /**
         * Whether or not location is required by the calling app in order to continue. Set this to
         * true if location is required to continue and false if having location provides better
         * results, but is not required. This changes the wording/appearance of the dialog
         * accordingly.
         */
        public Builder setAlwaysShow(boolean show) {
            alwaysShow = show;
            return this;
        }

        /**
         * Sets whether the client wants BLE scan to be enabled. When this flag is set to true, if
         * the platform supports BLE scan mode and Bluetooth is off, the dialog will prompt the
         * user to enable BLE scan. If the platform doesn't support BLE scan mode, the dialog will
         * prompt to enable Bluetooth.
         */
        public Builder setNeedBle(boolean needBle) {
            this.needBle = needBle;
            return this;
        }
    }

    @Hide
    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LocationSettingsRequest")
                .value(requests)
                .field("alwaysShow", alwaysShow)
                .field("needBle", needBle)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<LocationSettingsRequest> CREATOR = findCreator(LocationSettingsRequest.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
