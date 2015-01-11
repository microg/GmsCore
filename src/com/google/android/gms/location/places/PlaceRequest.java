package com.google.android.gms.location.places;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class PlaceRequest implements SafeParcelable {
    private PlaceRequest(Parcel in) {
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

    public static final Creator<PlaceRequest> CREATOR = new Creator<PlaceRequest>() {
        @Override
        public PlaceRequest createFromParcel(Parcel parcel) {
            return new PlaceRequest(parcel);
        }

        @Override
        public PlaceRequest[] newArray(int i) {
            return new PlaceRequest[i];
        }
    };
}
