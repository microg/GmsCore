package com.google.android.gms.location;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

public class LocationStatus extends AutoSafeParcelable {
    public static final int STATUS_SUCCESSFUL = 0;
    public static final int STATUS_UNKNOWN = 1;
    public static final int STATUS_TIMED_OUT_ON_SCAN = 2;
    public static final int STATUS_NO_INFO_IN_DATABASE = 3;
    public static final int STATUS_INVALID_SCAN = 4;
    public static final int STATUS_UNABLE_TO_QUERY_DATABASE = 5;
    public static final int STATUS_SCANS_DISABLED_IN_SETTINGS = 6;
    public static final int STATUS_LOCATION_DISABLED_IN_SETTINGS = 7;
    public static final int STATUS_IN_PROGRESS = 8;
    @SafeParceled(1000)
    private final int versionCode;
    @SafeParceled(1)
    int cellStatus;
    @SafeParceled(2)
    int wifiStatus;
    @SafeParceled(3)
    long elapsedRealtimeNanos;

    private LocationStatus() {
        versionCode = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LocationStatus that = (LocationStatus) o;

        if (cellStatus != that.cellStatus)
            return false;
        if (elapsedRealtimeNanos != that.elapsedRealtimeNanos)
            return false;
        if (wifiStatus != that.wifiStatus)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { cellStatus, wifiStatus, elapsedRealtimeNanos });
    }

    private String statusToString(int status) {
        switch (status) {
            case STATUS_SUCCESSFUL:
                return "STATUS_SUCCESSFUL";
            case STATUS_UNKNOWN:
            default:
                return "STATUS_UNKNOWN";
            case STATUS_TIMED_OUT_ON_SCAN:
                return "STATUS_TIMED_OUT_ON_SCAN";
            case STATUS_NO_INFO_IN_DATABASE:
                return "STATUS_NO_INFO_IN_DATABASE";
            case STATUS_INVALID_SCAN:
                return "STATUS_INVALID_SCAN";
            case STATUS_UNABLE_TO_QUERY_DATABASE:
                return "STATUS_UNABLE_TO_QUERY_DATABASE";
            case STATUS_SCANS_DISABLED_IN_SETTINGS:
                return "STATUS_SCANS_DISABLED_IN_SETTINGS";
            case STATUS_LOCATION_DISABLED_IN_SETTINGS:
                return "STATUS_LOCATION_DISABLED_IN_SETTINGS";
            case STATUS_IN_PROGRESS:
                return "STATUS_IN_PROGRESS";
        }
    }

    public static final Creator<LocationStatus> CREATOR = new AutoCreator<LocationStatus>(LocationStatus.class);
}
