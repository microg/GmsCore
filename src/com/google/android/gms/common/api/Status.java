package com.google.android.gms.common.api;

import android.app.PendingIntent;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Represents the results of work.
 * <p/>
 * TODO: Docs
 */
public final class Status extends AutoSafeParcelable implements Result {
    private static final int STATUS_CODE_INTERRUPTED = 14;
    private static final int STATUS_CODE_CANCELED = 16;

    public static final Status INTERRUPTED = new Status(STATUS_CODE_INTERRUPTED);
    public static final Status CANCELED = new Status(STATUS_CODE_CANCELED);

    private final int versionCode;
    private final int statusCode;
    private final String statusMessage;
    private final PendingIntent resolution;

    private Status() {
        versionCode = 1;
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
        this.versionCode = 1;
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

    public static final Creator<Status> CREATOR = new AutoCreator<>(Status.class);
}
