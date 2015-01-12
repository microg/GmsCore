package com.google.android.gms.location.places.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class PlacesParams extends AutoSafeParcelable {

    @SafeParceled(1000)
    private final int versionCode;
    @SafeParceled(1)
    public final String clientPackageName;
    @SafeParceled(2)
    public final String locale;
    @SafeParceled(3)
    public final String accountName;
    @SafeParceled(4)
    public final String gCoreClientName;

    private PlacesParams() {
        versionCode = 1;
        clientPackageName = locale = accountName = gCoreClientName = null;
    }

    public static final Creator<PlacesParams> CREATOR = new AutoCreator<>(PlacesParams.class);
}
