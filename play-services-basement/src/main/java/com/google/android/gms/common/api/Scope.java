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

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.PublicApi;

/**
 * Describes an OAuth 2.0 scope to request. This has security implications for the user, and
 * requesting additional scopes will result in authorization dialogs.
 */
@PublicApi
@SafeParcelable.Class
public class Scope extends AbstractSafeParcelable {
    @Field(1)
    int versionCode = 1;
    @Field(value = 2, getterName = "getScopeUri")
    private final String scopeUri;

    private Scope() {
        scopeUri = null;
    }

    /**
     * Creates a new scope with the given URI.
     */
    @Constructor
    public Scope(@Param(2) String scopeUri) {
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

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Scope> CREATOR = findCreator(Scope.class);
}
