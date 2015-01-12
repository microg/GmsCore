package com.google.android.gms.location.places;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * TODO: usage
 */
public class UserAddedPlace extends AutoSafeParcelable {

    @SafeParceled(1000)
    private final int versionCode;

    private UserAddedPlace() {
        versionCode = 1;
    }

    public static final Creator<UserAddedPlace> CREATOR = new AutoCreator<>(UserAddedPlace.class);
}
