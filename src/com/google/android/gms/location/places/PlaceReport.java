package com.google.android.gms.location.places;

import android.os.Parcel;
import android.os.Parcelable;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class PlaceReport implements SafeParcelable {

    private PlaceReport(Parcel in) {
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

    public static final Parcelable.Creator<PlaceReport> CREATOR = new Parcelable.Creator<PlaceReport>() {
        @Override
        public PlaceReport createFromParcel(Parcel parcel) {
            return new PlaceReport(parcel);
        }

        @Override
        public PlaceReport[] newArray(int i) {
            return new PlaceReport[i];
        }
    };
}
