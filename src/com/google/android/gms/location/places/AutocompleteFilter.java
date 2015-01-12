package com.google.android.gms.location.places;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * TODO usage
 */
public class AutocompleteFilter extends AutoSafeParcelable {

    @SafeParceled(1000)
    private final int versionCode;

    private AutocompleteFilter() {
        this.versionCode = 1;
    }

    public static final Creator<AutocompleteFilter> CREATOR = new AutoCreator<>(
            AutocompleteFilter.class);
}
