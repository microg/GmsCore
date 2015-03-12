package com.google.android.gms.common.api;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Describes an OAuth 2.0 scope to request. This has security implications for the user, and
 * requesting additional scopes will result in authorization dialogs.
 */
@PublicApi
public class Scope extends AutoSafeParcelable {
    @SafeParceled(1)
    private final int versionCode;
    @SafeParceled(2)
    private final String scopeUri;

    private Scope() {
        versionCode = -1;
        scopeUri = null;
    }

    /**
     * Creates a new scope with the given URI.
     */
    public Scope(String scopeUri) {
        versionCode = 1;
        this.scopeUri = scopeUri;
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

    public static final Creator<Scope> CREATOR = new AutoCreator<>(Scope.class);
}
