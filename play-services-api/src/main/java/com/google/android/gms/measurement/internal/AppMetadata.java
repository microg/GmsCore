/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import androidx.annotation.Nullable;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class AppMetadata extends AutoSafeParcelable {
    @Field(2)
    @Nullable
    public String packageName;
    @Field(3)
    @Nullable
    public String appId;
    @Field(4)
    @Nullable
    public String appVersion;
    @Field(5)
    @Nullable
    public String appStore;
    @Field(6)
    public long gmpVersion;
    @Field(7)
    public long devCertHash;
    @Field(8)
    @Nullable
    public String healthMonitorSample;
    @Field(value = 9, defaultValue = "true")
    public boolean measurementEnabled = true;
    @Field(10)
    public boolean firstOpen;
    @Field(value = 11, defaultValue = "java.lang.Integer.MIN_VALUE")
    public long appVersionInt = Integer.MIN_VALUE;
    @Field(12)
    @Nullable
    public String firebaseInstanceId;
    @Field(13)
    public long androidId;
    @Field(14)
    public long instantiationTime;
    @Field(15)
    public int appType;
    @Field(value = 16, defaultValue = "true")
    public boolean adIdReportingEnabled = true;
    @Field(value = 17, defaultValue = "true")
    public boolean ssaidCollectionEnabled = true;
    @Field(18)
    public boolean deferredAnalyticsCollection;
    @Field(19)
    public String admobAppId;
    @Field(21)
    @Nullable
    public Boolean allowAdPersonalization;
    @Field(22)
    public long dynamiteVersion;
    @Field(23)
    @Nullable
    public List<String> safelistedEvents;
    @Field(24)
    public String gaAppId;
    @Field(value = 25, defaultValue = "\"\"")
    public String consentSettings = "";
    @Field(value = 26, defaultValue = "\"\"")
    public String ephemeralAppInstanceId = "";
    @Field(27)
    @Nullable
    public String sessionStitchingToken;
    @Field(28)
    public boolean sgtmUploadEnabled = false;
    @Field(29)
    public long targetOsVersion;
    @Field(value = 30, defaultValue = "100")
    public int consentSource = 100;
    @Field(value = 31, defaultValue = "\"\"")
    public String dmaConsent = "";
    @Field(32)
    public int adServicesVersion;
    @Field(34)
    public long l34;
    @Field(35)
    @Nullable
    public String sgtmPreviewKey;
    @Field(value = 36, defaultValue = "\"\"")
    public String serializedNpaMetadata;
    @Field(37)
    public long timestamp;
    @Field(38)
    public int clientUploadEligibility;

    public String toString() {
        return "AppMetadata[" + packageName + "]";
    }

    public static final Creator<AppMetadata> CREATOR = new AutoCreator<>(AppMetadata.class);
}
