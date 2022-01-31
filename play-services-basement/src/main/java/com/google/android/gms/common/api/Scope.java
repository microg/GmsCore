/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private final int versionCode = 1;
    @SafeParceled(2)
    private final String scopeUri;

    private Scope() {
        scopeUri = null;
    }

    /**
     * Creates a new scope with the given URI.
     */
    public Scope(String scopeUri) {
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
