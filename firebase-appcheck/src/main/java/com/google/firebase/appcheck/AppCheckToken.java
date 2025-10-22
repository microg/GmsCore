/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.appcheck;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Represents a Firebase App Check token.
 */
@PublicApi
public class AppCheckToken extends AutoSafeParcelable {
    @Field(1)
    @PublicApi(exclude = true)
    public String token;
    
    @Field(2)
    @PublicApi(exclude = true)
    public long expireTimeMillis;

    public AppCheckToken() {
    }

    public AppCheckToken(String token, long expireTimeMillis) {
        this.token = token;
        this.expireTimeMillis = expireTimeMillis;
    }

    /**
     * Returns the raw App Check token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the expire time of the App Check token in milliseconds since epoch.
     */
    public long getExpireTimeMillis() {
        return expireTimeMillis;
    }

    public static final Creator<AppCheckToken> CREATOR = new AutoCreator<>(AppCheckToken.class);
}