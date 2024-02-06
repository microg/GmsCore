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
    private long googleVersion;
    @Field(7)
    private long devCertHash;
    @Field(8)
    private String healthMonitor;
    @Field(9)
    private boolean measurementEnabled = true;
    @Field(10)
    private boolean firstOpen;
    @Field(11)
    public long versionCode = Integer.MIN_VALUE;
    @Field(12)
    public String firebaseInstanceId;
    @Field(13)
    private long androidId;
    @Field(14)
    private long instantiationTime;
    @Field(15)
    public int appType;
    @Field(16)
    private boolean adIdReportingEnabled;
    @Field(17)
    public boolean ssaidCollectionEnabled = true;
    @Field(18)
    public boolean deferredAnalyticsCollection;
    @Field(19)
    public String admobAppId;
    @Field(21)
    public Boolean allowAdPersonalization;
    @Field(22)
    private long dynamiteVersion;
    @Field(23)
    public List<String> safelistedEvents;
    @Field(24)
    public String gaAppId;
    @Field(25)
    private String consentSettings;
    @Field(26)
    public String ephemeralAppInstanceId;
    @Field(27)
    private String sessionStitchingToken;
    @Field(28)
    private boolean sgtmUploadEnabled;
    @Field(29)
    private long targetOsVersion;
    @Field(30)
    private int consentSource;
    @Field(31)
    private String dmaConsent;
    @Field(32)
    private int adServicesVersion;

    public String toString() {
        return "AppMetadata[" + packageName + "]";
    }

    public static final Creator<AppMetadata> CREATOR = new AutoCreator<>(AppMetadata.class);
}
