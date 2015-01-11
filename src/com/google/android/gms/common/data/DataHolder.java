package com.google.android.gms.common.data;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class DataHolder implements SafeParcelable {
    private DataHolder(Parcel in) {
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

    public static final Creator<DataHolder> CREATOR = new Creator<DataHolder>() {
        @Override
        public DataHolder createFromParcel(Parcel parcel) {
            return new DataHolder(parcel);
        }

        @Override
        public DataHolder[] newArray(int i) {
            return new DataHolder[i];
        }
    };
}
