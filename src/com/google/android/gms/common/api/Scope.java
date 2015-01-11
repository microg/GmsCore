package com.google.android.gms.common.api;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * Describes an OAuth 2.0 scope to request. This has security implications for the user, and 
 * requesting additional scopes will result in authorization dialogs.
 */
public class Scope implements SafeParcelable {
    private final int versionCode;
    private final String scopeUri;

    private Scope() {
        versionCode = -1;
        scopeUri = null;
    }

    private Scope(Parcel in) {
        this();
        SafeParcelUtil.readObject(this, in);
    }

    /**
     * Creates a new scope with the given URI.
     */
    public Scope(String scopeUri) {
        versionCode = 1;
        this.scopeUri = scopeUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Scope && scopeUri.equals(((Scope) o).scopeUri);
    }

    public String getScopeUri() {
        return scopeUri;
    }

    @Override
    public int hashCode() {
        return scopeUri.hashCode();
    }

    @Override
    public String toString() {
        return scopeUri;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        SafeParcelUtil.writeObject(this, out, flags);
    }

    public static final Creator<Scope> CREATOR = new Creator<Scope>() {
        @Override
        public Scope createFromParcel(Parcel parcel) {
            return new Scope(parcel);
        }

        @Override
        public Scope[] newArray(int i) {
            return new Scope[i];
        }
    };
}
