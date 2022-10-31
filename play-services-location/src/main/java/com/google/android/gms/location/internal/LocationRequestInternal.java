/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import com.google.android.gms.location.LocationRequest;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class LocationRequestInternal extends AutoSafeParcelable {

    @Field(1000)
    private int versionCode = 1;

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
