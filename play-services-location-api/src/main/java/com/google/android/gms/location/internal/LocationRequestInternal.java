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

import java.util.List;

public class LocationRequestInternal extends AutoSafeParcelable {

    @Field(1000)
    private final int versionCode = 1;

    @Field(1)
    public LocationRequest request;

    @Field(2) @Deprecated
    public boolean requestNlpDebugInfo;

    @Field(3) @Deprecated
    public boolean restorePendingIntentListeners;

    @Field(4) @Deprecated
    public boolean triggerUpdate;

    @Field(value = 5, subClass = ClientIdentity.class)
    public List<ClientIdentity> clients;

    @Field(6)
    public String tag;

    @Field(7)
    public boolean hideFromAppOps;

    @Field(8)
    public boolean forceCoarseLocation;

    @Field(9)
    public boolean exemptFromThrottle;

    @Field(10)
    public String moduleId;

    @Field(11)
    public boolean locationSettingsIgnored;

    @Field(12)
    public boolean inaccurateLocationsDelayed;

    @Field(13)
    public String contextAttributeTag;

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
                ", locationSettingsIgnored=" + locationSettingsIgnored +
                ", inaccurateLocationsDelayed=" + inaccurateLocationsDelayed +
                ", contextAttributeTag=" + contextAttributeTag +
                '}';
    }

    public static final Creator<LocationRequestInternal> CREATOR = new AutoCreator<LocationRequestInternal>(LocationRequestInternal.class);
}
