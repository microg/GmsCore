package com.google.android.gms.location.places;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * TODO: usage
 */
public class UserAddedPlace implements SafeParcelable {
    
    @SafeParceled(1000)
    private final int versionCode;
    
    private UserAddedPlace() {
        versionCode = -1;
    }

    private UserAddedPlace(Parcel in) {
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
    
    public static final Creator<UserAddedPlace> CREATOR = new Creator<UserAddedPlace>() {
        @Override
        public UserAddedPlace createFromParcel(Parcel parcel) {
            return new UserAddedPlace(parcel);
        }

        @Override
        public UserAddedPlace[] newArray(int i) {
            return new UserAddedPlace[i];
        }
    };
}
