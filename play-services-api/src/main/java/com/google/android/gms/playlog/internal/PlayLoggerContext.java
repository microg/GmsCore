/*
 * Copyright 2013-2015 µg Project Team
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
    public final int versionCode;

    @SafeParceled(2)
    public final String packageName;

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

    private PlayLoggerContext() {
        this.versionCode = 1;
        packageName = uploadAccount = logSourceName = null;
        logSource = loggingId = -1;
        logAndroidId = false;
    }

    public PlayLoggerContext(String packageName, int logSource, String uploadAccount, int loggingId, String logSourceName) {
        this.versionCode = 1;
        this.packageName = packageName;
        this.logSource = logSource;
        this.uploadAccount = uploadAccount;
        this.loggingId = loggingId;
        this.logSourceName = logSourceName;
        this.logAndroidId = true;
    }

    @Override
    public String toString() {
        return "PlayLoggerContext{" +
                "packageName='" + packageName + '\'' +
                ", logSource=" + logSource +
                ", uploadAccount='" + uploadAccount + '\'' +
                ", loggingId=" + loggingId +
                ", logAndroidId=" + logAndroidId +
                ", logSourceName='" + logSourceName + '\'' +
                '}';
    }

    public static Creator<PlayLoggerContext> CREATOR = new AutoCreator<>(PlayLoggerContext.class);
}
