/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PublicApi
public class LocationSettingsRequest extends AutoSafeParcelable {
    @Field(1000)
    private int versionCode = 2;

    @Field(value = 1, subClass = LocationRequest.class)
    @PublicApi(exclude = true)
    public List<LocationRequest> requests;

    @Field(2)
    @PublicApi(exclude = true)
    public boolean alwaysShow;

    @Field(3)
    @PublicApi(exclude = true)
    public boolean needBle;

    @Field(5)
    @PublicApi(exclude = true)
    public LocationSettingsConfiguration configuration;

    private LocationSettingsRequest() {
    }

    private LocationSettingsRequest(List<LocationRequest> requests, boolean alwaysShow, boolean needBle, LocationSettingsConfiguration configuration) {
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

    public static final Creator<LocationSettingsRequest> CREATOR = new AutoCreator<LocationSettingsRequest>(LocationSettingsRequest.class);
}
