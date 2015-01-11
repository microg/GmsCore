package com.google.android.gms.location.places;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * TODO usage
 */
public class AutocompleteFilter implements SafeParcelable {
    
    @SafeParceled(1000)
    private final int versionCode;

    private AutocompleteFilter() {
        this.versionCode = 1;
    }

    private AutocompleteFilter(Parcel in) {
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
    
    public static final Creator<AutocompleteFilter> CREATOR = new Creator<AutocompleteFilter>() {
        @Override
        public AutocompleteFilter createFromParcel(Parcel parcel) {
            return new AutocompleteFilter(parcel);
        }

        @Override
        public AutocompleteFilter[] newArray(int i) {
            return new AutocompleteFilter[i];
        }
    };
}
