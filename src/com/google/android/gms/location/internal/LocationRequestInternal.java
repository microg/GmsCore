package com.google.android.gms.location.internal;

import android.os.Parcel;
import android.os.Parcelable;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class LocationRequestInternal implements SafeParcelable {
    private LocationRequestInternal(Parcel in) {
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

    public static final Parcelable.Creator<LocationRequestInternal> CREATOR = new Parcelable.Creator<LocationRequestInternal>() {
        @Override
        public LocationRequestInternal createFromParcel(Parcel parcel) {
            return new LocationRequestInternal(parcel);
        }

        @Override
        public LocationRequestInternal[] newArray(int i) {
            return new LocationRequestInternal[i];
        }
    };
}
