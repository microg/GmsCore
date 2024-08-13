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

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

import java.util.List;

@Hide
@SafeParcelable.Class
public class TokenData extends AbstractSafeParcelable {
    @Field(value = 1, versionCode = 1)
    private int versionCode = 1;

    @Field(value = 2)
    private final String token;

    @Field(value = 3)
    private final Long expirationTimeSecs;

    @Field(value = 4)
    private final boolean isCached;

    @Field(value = 5)
    private final boolean isSnowballed;

    @Field(value = 6)
    private final List<String> grantedScopes;

    @Field(value = 7)
    private final String scopeData;

    @Nullable
    public static TokenData getTokenData(Bundle extras, String key) {
        Bundle tokenBundle;
        if ((tokenBundle = extras.getBundle(key)) == null) {
            return null;
        } else {
            tokenBundle.setClassLoader(TokenData.class.getClassLoader());
            return tokenBundle.getParcelable("TokenData");
        }
    }

    @Constructor
    public TokenData(@Param(value = 1) int versionCode, @Param(value = 2) String token, @Param(value = 3) Long expirationTimeSecs,
                     @Param(value = 4) boolean isCached, @Param(value = 5) boolean isSnowballed, @Param(value = 6) List<String> grantedScopes,
                     @Param(value = 7) String scopeData) {
        this.versionCode = versionCode;
        this.token = token;
        this.expirationTimeSecs = expirationTimeSecs;
        this.isCached = isCached;
        this.isSnowballed = isSnowballed;
        this.grantedScopes = grantedScopes;
        this.scopeData = scopeData;
    }

    public Long getExpirationTimeSecs() {
        return expirationTimeSecs;
    }

    public boolean isCached() {
        return isCached;
    }

    public boolean isSnowballed() {
        return isSnowballed;
    }

    public List<String> getGrantedScopes() {
        return grantedScopes;
    }

    public String getScopeData() {
        return scopeData;
    }

    public String getToken() {
        return this.token;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<TokenData> CREATOR = findCreator(TokenData.class);
}
