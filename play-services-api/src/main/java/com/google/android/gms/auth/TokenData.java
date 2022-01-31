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

package com.google.android.gms.auth;

import com.google.android.gms.common.api.Scope;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

public class TokenData extends AutoSafeParcelable {
    @SafeParceled(1)
    private final int versionCode = 1;

    @SafeParceled(2)
    public final String token;

    @SafeParceled(3)
    public final Long expiry;

    @SafeParceled(5)
    public final boolean isOAuth;

    @SafeParceled(value = 6, subClass = Scope.class)
    public final List<Scope> scopes;

    public TokenData() {
        token = null;
        expiry = null;
        isOAuth = false;
        scopes = null;
    }

    public TokenData(String token, Long expiry, boolean isOAuth, List<Scope> scopes) {
        this.token = token;
        this.expiry = expiry;
        this.isOAuth = isOAuth;
        this.scopes = scopes;
    }

    public TokenData(String token, Long expiry) {
        this.token = token;
        this.expiry = expiry;
        this.isOAuth = false;
        this.scopes = null;
    }

    public static final Creator<TokenData> CREATOR = new AutoCreator<TokenData>(TokenData.class);
}
