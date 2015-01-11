package com.google.android.gms.location.places.internal;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class PlacesParams implements SafeParcelable {

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
        versionCode = -1;
        clientPackageName = locale = accountName = gCoreClientName = null;
    }

    private PlacesParams(Parcel in) {
        this();
        SafeParcelUtil.readObject(this, in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        SafeParcelUtil.writeObject(this, out, flags);
    }
    
    public static final Creator<PlacesParams> CREATOR = new Creator<PlacesParams>() {
        @Override
        public PlacesParams createFromParcel(Parcel parcel) {
            return new PlacesParams(parcel);
        }

        @Override
        public PlacesParams[] newArray(int i) {
            return new PlacesParams[i];
        }
    };
}
