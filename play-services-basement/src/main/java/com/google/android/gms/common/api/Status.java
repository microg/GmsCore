/*
 * Copyright 2013-2015 microG Project Team
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

package com.google.android.gms.common.api;

import android.app.PendingIntent;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Represents the results of work.
 * <p/>
 * TODO: Docs
 */
@PublicApi
public final class Status extends AutoSafeParcelable implements Result {
    private static final int STATUS_CODE_INTERNAL_ERROR = 8;
    private static final int STATUS_CODE_INTERRUPTED = 14;
    private static final int STATUS_CODE_CANCELED = 16;

    public static final Status INTERNAL_ERROR = new Status(STATUS_CODE_INTERNAL_ERROR);
    public static final Status INTERRUPTED = new Status(STATUS_CODE_INTERRUPTED);
    public static final Status CANCELED = new Status(STATUS_CODE_CANCELED);
    public static final Status SUCCESS = new Status(0);

    private int versionCode = 1;
    private final int statusCode;
    private final String statusMessage;
    private final PendingIntent resolution;

    private Status() {
        statusCode = 0;
        statusMessage = null;
        resolution = null;
    }

    public Status(int statusCode) {
        this(statusCode, null);
    }

    public Status(int statusCode, String statusMessage) {
        this(statusCode, statusMessage, null);
    }

    public Status(int statusCode, String statusMessage, PendingIntent resolution) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.resolution = resolution;
    }

    public PendingIntent getResolution() {
        return resolution;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean hasResolution() {
        return resolution != null;
    }

    public boolean isCanceled() {
        return statusCode == STATUS_CODE_CANCELED;
    }

    public boolean isInterrupted() {
        return statusCode == STATUS_CODE_INTERRUPTED;
    }

    public boolean isSuccess() {
        return statusCode <= 0;
    }

    @Override
    public Status getStatus() {
        return this;
    }

    public static final Creator<Status> CREATOR = new AutoCreator<Status>(Status.class);
}
