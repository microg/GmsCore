package com.google.android.gms.location.places;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class PlaceFilter implements SafeParcelable {

    private PlaceFilter(Parcel in) {
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

    public static final Creator<PlaceFilter> CREATOR = new Creator<PlaceFilter>() {
        @Override
        public PlaceFilter createFromParcel(Parcel parcel) {
            return new PlaceFilter(parcel);
        }

        @Override
        public PlaceFilter[] newArray(int i) {
            return new PlaceFilter[i];
        }
    };
}
