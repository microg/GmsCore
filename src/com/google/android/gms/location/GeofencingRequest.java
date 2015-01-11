package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class GeofencingRequest implements SafeParcelable {

    private GeofencingRequest(Parcel in) {
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

    public static final Parcelable.Creator<GeofencingRequest> CREATOR = new Parcelable.Creator<GeofencingRequest>() {
        @Override
        public GeofencingRequest createFromParcel(Parcel parcel) {
            return new GeofencingRequest(parcel);
        }

        @Override
        public GeofencingRequest[] newArray(int i) {
            return new GeofencingRequest[i];
        }
    };
}
