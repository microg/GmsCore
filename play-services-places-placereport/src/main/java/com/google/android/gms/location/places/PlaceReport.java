/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.places;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

@PublicApi
public class PlaceReport extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode;
    @SafeParceled(2)
    private String placeId;
    @SafeParceled(3)
    private String tag;
    @SafeParceled(4)
    private String source;

    public String getPlaceId() {
        return placeId;
    }

    public String getTag() {
        return tag;
    }

    public static final Creator<PlaceReport> CREATOR = new AutoCreator<PlaceReport>(PlaceReport.class);
}
