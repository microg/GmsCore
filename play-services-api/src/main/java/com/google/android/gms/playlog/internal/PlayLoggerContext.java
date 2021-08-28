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

package com.google.android.gms.playlog.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class PlayLoggerContext extends AutoSafeParcelable {

    @Field(1)
    private int versionCode = 1;

    @Field(2)
    public final String packageName;

    @Field(3)
    public final int packageVersionCode;

    @Field(4)
    public final int logSource;

    @Field(8)
    public final String logSourceName;

    @Field(5)
    public final String uploadAccount;

    @Field(6)
    public final String loggingId;

    @Field(7)
    public final boolean logAndroidId;

    @Field(9)
    public final boolean isAnonymous;

    @Field(10)
    public final int qosTier;

    @Field(11)
    public final Integer appMobileSpecId;

    @Field(12)
    public final boolean scrubMccMnc;

    private PlayLoggerContext() {
        packageName = uploadAccount = logSourceName = loggingId = null;
        qosTier = packageVersionCode = logSource = appMobileSpecId = -1;
        isAnonymous = logAndroidId = scrubMccMnc = false;
    }

    public PlayLoggerContext(String packageName, int packageVersionCode, int logSource, String logSourceName, String uploadAccount, String loggingId, boolean isAnonymous,  int qosTier, boolean scrubMccMnc) {
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
        sb.append(']');
        return sb.toString();
    }

    public static Creator<PlayLoggerContext> CREATOR = new AutoCreator<PlayLoggerContext>(PlayLoggerContext.class);
}
