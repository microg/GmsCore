package com.google.android.gms.common.data;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * TODO: usage
 */
public class DataHolder extends AutoSafeParcelable {

    public static final Creator<DataHolder> CREATOR = new AutoCreator<>(DataHolder.class);
}
