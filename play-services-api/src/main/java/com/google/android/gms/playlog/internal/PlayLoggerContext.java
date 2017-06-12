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

    @SafeParceled(1)
    private int versionCode = 1;

    @SafeParceled(2)
    public final String packageName;

    @SafeParceled(3)
    public final int packageVersionCode;

    @SafeParceled(4)
    public final int logSource;

    @SafeParceled(5)
    public final String uploadAccount;

    @SafeParceled(6)
    public final int loggingId;

    @SafeParceled(7)
    public final boolean logAndroidId;

    @SafeParceled(8)
    public final String logSourceName;

    @SafeParceled(9)
    public final boolean isAnonymous;

    @SafeParceled(10)
    public final int qosTier;

    private PlayLoggerContext() {
        packageName = uploadAccount = logSourceName = null;
        qosTier = packageVersionCode = logSource = loggingId = -1;
        isAnonymous = logAndroidId = false;
    }

    public PlayLoggerContext(String packageName, int packageVersionCode, int logSource, String uploadAccount, int loggingId, boolean logAndroidId) {
        this.packageName = packageName;
        this.packageVersionCode = packageVersionCode;
        this.logSource = logSource;
        this.logSourceName = null;
        this.uploadAccount = uploadAccount;
        this.loggingId = loggingId;
        this.logAndroidId = logAndroidId;
        this.isAnonymous = false;
        this.qosTier = 0;
    }

    public PlayLoggerContext(String packageName, int packageVersionCode, int logSource, String logSourceName, String uploadAccount, int loggingId, boolean isAnonymous, int qosTier) {
        this.packageName = packageName;
        this.packageVersionCode = packageVersionCode;
        this.logSource = logSource;
        this.logSourceName = logSourceName;
        this.uploadAccount = uploadAccount;
        this.loggingId = loggingId;
        this.logAndroidId = !isAnonymous;
        this.isAnonymous = isAnonymous;
        this.qosTier = qosTier;
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
