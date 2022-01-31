/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.clearcut.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class PlayLoggerContext extends AutoSafeParcelable {

    @Field(1)
    private final int versionCode = 1;

    @Field(2)
    public final String packageName;

    @Field(3)
    public final int packageVersionCode;

    @Field(4)
    public final int logSource;

    @Field(5)
    public final String uploadAccount;

    @Field(6)
    public final String loggingId;

    @Field(7)
    public final boolean logAndroidId;

    @Field(8)
    public final String logSourceName;

    @Field(9)
    public final boolean isAnonymous;

    @Field(10)
    public final int qosTier;

    @Field(11)
    public final Integer appMobileSpecId;

    @Field(12)
    public final boolean scrubMccMnc;

    @Field(13)
    public final Integer piiLevelset;

    private PlayLoggerContext() {
        packageName = uploadAccount = logSourceName = loggingId = null;
        qosTier = packageVersionCode = logSource = appMobileSpecId = piiLevelset = -1;
        isAnonymous = logAndroidId = scrubMccMnc = false;
    }

    public PlayLoggerContext(String packageName, int packageVersionCode, int logSource, String logSourceName, String uploadAccount, String loggingId, boolean isAnonymous,  int qosTier, boolean scrubMccMnc, int piiLevelset) {
        this.packageName = packageName;
        this.packageVersionCode = packageVersionCode;
        this.logSource = logSource;
        this.logSourceName = logSourceName;
        this.uploadAccount = uploadAccount;
        this.loggingId = loggingId;
        this.logAndroidId = !isAnonymous;
        this.isAnonymous = isAnonymous;
        this.qosTier = qosTier;
        this.appMobileSpecId = null;
        this.scrubMccMnc = scrubMccMnc;
        this.piiLevelset = piiLevelset;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PlayLoggerContext[").append(versionCode);
        sb.append(", package=").append(packageName);
        sb.append(", packageVersionCode=").append(packageVersionCode);
        sb.append(", logSource=").append(logSource);
        sb.append(", uploadAccount=").append(uploadAccount);
        sb.append(", loggingId=").append(loggingId);
        sb.append(", logAndroidId=").append(logAndroidId);
        sb.append(", logSourceName=").append(logSourceName);
        sb.append(", isAnonymous=").append(isAnonymous);
        sb.append(", qosTier=").append(qosTier);
        sb.append(", appMobileSpecId=").append(appMobileSpecId);
        sb.append(", scrubMccMnc=").append(scrubMccMnc);
        sb.append(", piiLevelset=").append(piiLevelset);
        sb.append(']');
        return sb.toString();
    }

    public static Creator<PlayLoggerContext> CREATOR = new AutoCreator<PlayLoggerContext>(PlayLoggerContext.class);
}
