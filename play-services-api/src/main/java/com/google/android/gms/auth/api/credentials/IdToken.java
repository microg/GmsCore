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

package com.google.android.gms.auth.api.credentials;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

@PublicApi
public class IdToken extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    private String accountType;

    @SafeParceled(2)
    private String id;

    private IdToken() {
    }

    public IdToken(String accountType, String id) {
        this.accountType = accountType;
        this.id = id;
    }

    /**
     * Returns {@code AccountManager} account type for the token.
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * Returns the ID token, formatted according to the rules defined by the account type.
     */
    public String getIdToken() {
        return id;
    }

    public static final Creator<IdToken> CREATOR = new AutoCreator<IdToken>(IdToken.class);
}
