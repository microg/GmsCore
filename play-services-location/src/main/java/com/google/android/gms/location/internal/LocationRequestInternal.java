/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import android.annotation.SuppressLint;
import android.os.WorkSource;
import com.google.android.gms.common.internal.ClientIdentity;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.ThrottleBehavior;
import org.microg.gms.utils.WorkSourceUtil;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class LocationRequestInternal extends AutoSafeParcelable {

    @Field(1000)
    private int versionCode = 1;

    @Field(1)
    public LocationRequest request;

    @Field(2)
    @Deprecated
    public boolean requestNlpDebugInfo;

    @Field(3)
    @Deprecated
    public boolean restorePendingIntentListeners;

    @Field(4)
    @Deprecated
    public boolean triggerUpdate;

    @Field(value = 5, subClass = ClientIdentity.class)
    @Deprecated
    public List<ClientIdentity> clients;

    @Field(6)
    @Deprecated
    public String tag;

    @Field(7)
    @Deprecated
    public boolean hideFromAppOps;

    @Field(8)
    @Deprecated
    public boolean forceCoarseLocation;

    @Field(9)
    @Deprecated
    public boolean exemptFromThrottle;

    @Field(10)
    @Deprecated
    public String moduleId;

    @Field(11)
    @Deprecated
    public boolean bypass;

    @Field(12)
    @Deprecated
    public boolean waitForAccurateLocation;

    @Field(13)
    @Deprecated
    public String contextAttributeTag;

    @Field(14)
    @Deprecated
    public long maxUpdateAgeMillis = Long.MAX_VALUE;

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
                ", locationSettingsIgnored=" + bypass +
                ", inaccurateLocationsDelayed=" + waitForAccurateLocation +
                ", contextAttributeTag=" + contextAttributeTag +
                '}';
    }

    @SuppressLint("MissingPermission")
    public LocationRequest getRequest() {
        LocationRequest.Builder builder = new LocationRequest.Builder(this.request);
        if (clients != null) {
            if (clients.isEmpty()) {
                builder.setWorkSource(null);
            } else {
                WorkSource workSource = new WorkSource();
                for (ClientIdentity client : clients) {
                    WorkSourceUtil.add(workSource, client.uid, client.packageName);
                }
            }
        }
        if (forceCoarseLocation) builder.setGranularity(Granularity.GRANULARITY_COARSE);
        if (exemptFromThrottle) builder.setThrottleBehavior(ThrottleBehavior.THROTTLE_NEVER);
        if (moduleId != null) builder.setModuleId(moduleId);
        else if (contextAttributeTag != null) builder.setModuleId(contextAttributeTag);
        if (bypass) builder.setBypass(true);
        if (waitForAccurateLocation) builder.setWaitForAccurateLocation(true);
        if (maxUpdateAgeMillis != Long.MAX_VALUE) builder.setMaxUpdateAgeMillis(maxUpdateAgeMillis);
        return builder.build();
    }

    public static final Creator<LocationRequestInternal> CREATOR = new AutoCreator<LocationRequestInternal>(LocationRequestInternal.class);
}
