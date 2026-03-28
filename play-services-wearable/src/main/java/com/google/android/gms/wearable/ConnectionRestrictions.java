package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

public class ConnectionRestrictions extends AutoSafeParcelable {
    @SafeParceled(1)
    public List<DataItemFilter> allowedDataItemFilters;
    @SafeParceled(2)
    public List<String> allowedCapabilities;
    @SafeParceled(3)
    public List<String> allowedPackages;

    private ConnectionRestrictions() {}

    public ConnectionRestrictions(List<DataItemFilter> allowedDataItemFilters,
                                  List<String> allowedCapabilities,
                                  List<String> allowedPackages) {
        this.allowedDataItemFilters = allowedDataItemFilters;
        this.allowedCapabilities = allowedCapabilities;
        this.allowedPackages = allowedPackages;
    }

    @Override
    public String toString() {
        return "ConnectionRestrictions{" +
                "allowedDataItemFilters=" + allowedDataItemFilters +
                ", allowedCapabilities=" + allowedCapabilities +
                ", allowedPackages=" + allowedPackages +
                '}';
    }

    public static final Creator<ConnectionRestrictions> CREATOR = new AutoCreator<>(ConnectionRestrictions.class);
}