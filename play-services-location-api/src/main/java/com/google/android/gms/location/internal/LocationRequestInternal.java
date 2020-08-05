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

package com.google.android.gms.location.internal;

import com.google.android.gms.location.LocationRequest;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

public class LocationRequestInternal extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    public LocationRequest request;

    @SafeParceled(2)
    public boolean requestNlpDebugInfo;

    @SafeParceled(3)
    public boolean restorePendingIntentListeners;

    @SafeParceled(4)
    public boolean triggerUpdate;

    @SafeParceled(value = 5, subClass = ClientIdentity.class)
    public List<ClientIdentity> clients;

    @SafeParceled(6)
    public String tag;

    @SafeParceled(7)
    public boolean hideFromAppOps;

    @SafeParceled(8)
    public boolean forceCoarseLocation;

    @SafeParceled(9)
    public boolean exemptFromThrottle;

    @SafeParceled(10)
    public String moduleId;

    @Override
    public String toString() {
        return "LocationRequestInternal{" +
                "request=" + request +
                ", requestNlpDebugInfo=" + requestNlpDebugInfo +
                ", restorePendingIntentListeners=" + restorePendingIntentListeners +
                ", triggerUpdate=" + triggerUpdate +
                ", clients=" + clients +
                ", tag='" + tag + '\'' +
                ", hideFromAppOps=" + hideFromAppOps +
                ", forceCoarseLocation=" + forceCoarseLocation +
                ", exemptFromThrottle=" + exemptFromThrottle +
                ", moduleId=" + moduleId +
                '}';
    }

    public static final Creator<LocationRequestInternal> CREATOR = new AutoCreator<LocationRequestInternal>(LocationRequestInternal.class);
}
