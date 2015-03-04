package com.google.android.gms.common.data;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class DataHolder extends AutoSafeParcelable {
    @SafeParceled(1000)
    private int versionCode = 1;

    public static final Creator<DataHolder> CREATOR = new AutoCreator<>(DataHolder.class);
}
