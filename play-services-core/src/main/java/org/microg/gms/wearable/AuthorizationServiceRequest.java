package org.microg.gms.wearable;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;

public class AuthorizationServiceRequest implements SafeParcelable {
    public String packageName;
    public String[] scopes;

    public AuthorizationServiceRequest() {}

    public AuthorizationServiceRequest(String packageName, String[] scopes) {
        this.packageName = packageName;
        this.scopes = scopes;
    }

    protected AuthorizationServiceRequest(Parcel in) {
        packageName = in.readString();
        scopes = in.createStringArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeStringArray(scopes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AuthorizationServiceRequest> CREATOR = new Creator<AuthorizationServiceRequest>() {
        @Override
        public AuthorizationServiceRequest createFromParcel(Parcel in) {
            return new AuthorizationServiceRequest(in);
        }

        @Override
        public AuthorizationServiceRequest[] newArray(int size) {
            return new AuthorizationServiceRequest[size];
        }
    };

    public static class Builder {
        private String packageName;
        private String[] scopes;

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder setScopes(String[] scopes) {
            this.scopes = scopes;
            return this;
        }

        public AuthorizationServiceRequest build() {
            return new AuthorizationServiceRequest(packageName, scopes);
        }
    }
}
