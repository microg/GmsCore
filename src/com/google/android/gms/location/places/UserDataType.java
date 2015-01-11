package com.google.android.gms.location.places;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * TODO: usage
 */
public class UserDataType implements SafeParcelable {
    private UserDataType(Parcel in) {
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

    public static final Creator<UserDataType> CREATOR = new Creator<UserDataType>() {
        @Override
        public UserDataType createFromParcel(Parcel parcel) {
            return new UserDataType(parcel);
        }

        @Override
        public UserDataType[] newArray(int i) {
            return new UserDataType[i];
        }
    };
}
