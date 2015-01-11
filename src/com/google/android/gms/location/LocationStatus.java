package com.google.android.gms.location;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

public class LocationStatus implements SafeParcelable {
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

    private LocationStatus(Parcel in) {
        versionCode = 1;
        SafeParcelUtil.readObject(this, in);
    }

    @Override
    public int describeContents() {
        return 0;
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

    @Override
    public void writeToParcel(Parcel out, int flags) {
        SafeParcelUtil.writeObject(this, out, flags);
    }

    public static final Creator<LocationStatus> CREATOR = new Creator<LocationStatus>() {
        @Override
        public LocationStatus createFromParcel(Parcel parcel) {
            return new LocationStatus(parcel);
        }

        @Override
        public LocationStatus[] newArray(int i) {
            return new LocationStatus[i];
        }
    };
}
