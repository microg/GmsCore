package org.microg.gms.wearable;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;

public class AuthorizationServiceResponse implements SafeParcelable {
    public int statusCode;
    public String authCode;

    public AuthorizationServiceResponse() {}

    public AuthorizationServiceResponse(int statusCode, String authCode) {
        this.statusCode = statusCode;
        this.authCode = authCode;
    }

    protected AuthorizationServiceResponse(Parcel in) {
        statusCode = in.readInt();
        authCode = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(statusCode);
        dest.writeString(authCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AuthorizationServiceResponse> CREATOR = new Creator<AuthorizationServiceResponse>() {
        @Override
        public AuthorizationServiceResponse createFromParcel(Parcel in) {
            return new AuthorizationServiceResponse(in);
        }

        @Override
        public AuthorizationServiceResponse[] newArray(int size) {
            return new AuthorizationServiceResponse[size];
        }
    };
}
