package com.google.android.gms.location.places;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class NearbyAlertRequest implements SafeParcelable {
    private NearbyAlertRequest(Parcel in) {
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

    public static final Creator<NearbyAlertRequest> CREATOR = new Creator<NearbyAlertRequest>() {
        @Override
        public NearbyAlertRequest createFromParcel(Parcel parcel) {
            return new NearbyAlertRequest(parcel);
        }

        @Override
        public NearbyAlertRequest[] newArray(int i) {
            return new NearbyAlertRequest[i];
        }
    };
}
