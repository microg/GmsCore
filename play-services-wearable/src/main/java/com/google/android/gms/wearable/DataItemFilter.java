package com.google.android.gms.wearable;

import android.net.Uri;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Objects;

public class DataItemFilter extends AutoSafeParcelable {
    @SafeParceled(1)
    public Uri uri;
    @SafeParceled(2)
    public int filterType;

    private DataItemFilter() {}

    public DataItemFilter(Uri uri, int filterType) {
        this.uri = uri;
        this.filterType = filterType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataItemFilter)) {
            return false;
        }
        DataItemFilter other = (DataItemFilter) obj;
        return Objects.equals(this.uri, other.uri) && this.filterType == other.filterType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, filterType);
    }

    @Override
    public String toString() {
        return "DataItemFilter{" +
                "uri=" + uri +
                ", filterType=" + filterType +
                '}';
    }

    public static final Creator<DataItemFilter> CREATOR = new AutoCreator<>(DataItemFilter.class);
}