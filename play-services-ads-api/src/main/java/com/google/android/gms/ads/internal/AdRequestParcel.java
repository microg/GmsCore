/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import android.location.Location;
import android.os.Bundle;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.List;

public class AdRequestParcel extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 8;
    @Field(2)
    public long birthday;
    @Field(3)
    public Bundle adMobNetworkExtras = new Bundle();
    @Field(4)
    public int gender;
    @Field(5)
    public ArrayList<String> keywords;
    @Field(6)
    public boolean isTestDevice;
    @Field(7)
    public int taggedForChildDirectedTreatment;
    @Field(9)
    public String publisherProvidedId;
    @Field(10)
    public SearchAdRequestParcel searchAdRequest;
    @Field(11)
    public Location location;
    @Field(12)
    public String contentUrl;
    @Field(13)
    public Bundle networkExtrasBundles = new Bundle();
    @Field(14)
    public Bundle customTargeting;
    @Field(15)
    public List<String> categoryExclusion;
    @Field(16)
    public String requestAgent;
    @Field(18)
    public boolean designedForFamilies;
    @Field(19)
    public AdDataParcel adData;
    @Field(20)
    public int tagForUnderAgeOfConsent;
    @Field(21)
    public String maxAdContentRating;
    @Field(22)
    public List<String> neighboringContentUrls;
    @Field(23)
    public int httpTimeoutMillis;
    @Field(24)
    public String adString;

    public static final Creator<AdRequestParcel> CREATOR = new AutoCreator<>(AdRequestParcel.class);
}
