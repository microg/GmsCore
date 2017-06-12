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

package com.google.android.gms.location;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PublicApi
public class LocationSettingsRequest extends AutoSafeParcelable {
    @SafeParceled(1000)
    private int versionCode = 2;

    @SafeParceled(value = 1, subClass = LocationRequest.class)
    @PublicApi(exclude = true)
    public List<LocationRequest> requests;

    @SafeParceled(2)
    @PublicApi(exclude = true)
    public boolean alwaysShow;

    @PublicApi(exclude = true)
    public boolean needBle;

    private LocationSettingsRequest() {
    }

    private LocationSettingsRequest(List<LocationRequest> requests, boolean alwaysShow, boolean needBle) {
        this.requests = requests;
        this.alwaysShow = alwaysShow;
        this.needBle = needBle;
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
            return new LocationSettingsRequest(requests, alwaysShow, needBle);
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
