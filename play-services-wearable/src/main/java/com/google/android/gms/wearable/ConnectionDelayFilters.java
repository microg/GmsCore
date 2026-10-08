package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;
import java.util.Objects;

public class ConnectionDelayFilters extends AutoSafeParcelable {
    @SafeParceled(1)
    public List<DataItemFilter> dataItemFilters;

    private ConnectionDelayFilters() {}

    public ConnectionDelayFilters(List<DataItemFilter> dataItemFilters) {
        this.dataItemFilters = dataItemFilters;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectionDelayFilters) {
            return Objects.equals(this.dataItemFilters, ((ConnectionDelayFilters) obj).dataItemFilters);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataItemFilters);
    }

    @Override
    public String toString() {
        return "ConnectionDelayFilters{" +
                "dataItemFilters=" + dataItemFilters +
                '}';
    }

    public static final Creator<ConnectionDelayFilters> CREATOR = new AutoCreator<>(ConnectionDelayFilters.class);
}