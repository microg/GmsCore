package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class PackageStorageInfo extends AutoSafeParcelable {
    @SafeParceled(1)
    private final int version = 1;
    @SafeParceled(2)
    public String packageName;
    @SafeParceled(3)
    public String appLabel;
    @SafeParceled(4)
    public long size;

    private PackageStorageInfo() {}

    public PackageStorageInfo(String packageName, String appLabel, long size) {
        this.packageName = packageName;
        this.appLabel = appLabel;
        this.size = size;
    }

    public static final Creator<PackageStorageInfo> CREATOR =
            new AutoCreator<>(PackageStorageInfo.class);
}