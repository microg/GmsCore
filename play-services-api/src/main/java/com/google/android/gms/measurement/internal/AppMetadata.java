/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class AppMetadata extends AutoSafeParcelable {
    @Field(2)
    public String packageName;
    @Field(3)
    public String googleAppId;
    @Field(4)
    public String versionName;
    @Field(5)
    public String installerPackageName;
    @Field(6)
    private long field6;
    @Field(7)
    private long field7;
    @Field(8)
    private String field8;
    @Field(9)
    private boolean field9 = true;
    @Field(10)
    private boolean field10;
    @Field(11)
    public long versionCode = Integer.MIN_VALUE;
    @Field(12)
    private String field12;
    @Field(13)
    private long field13;
    @Field(14)
    private long field14;
    @Field(15)
    public int appType;
    @Field(16)
    private boolean field16;
    @Field(17)
    public boolean ssaidCollectionEnabled = true;
    @Field(18)
    public boolean deferredAnalyticsCollection;
    @Field(19)
    public String admobAppId;
    @Field(21)
    public Boolean allowAdPersonalization;
    @Field(22)
    private long field22;
    @Field(23)
    public List<String> safelistedEvents;
    @Field(24)
    public String gaAppId;
    @Field(25)
    private String field25;

    public static final Creator<AppMetadata> CREATOR = new AutoCreator<>(AppMetadata.class);
}
