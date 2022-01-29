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

package com.google.android.gms.auth.firstparty.dataservice;

import android.accounts.Account;
import android.os.Bundle;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

// TODO
public class TokenRequest extends AutoSafeParcelable{

    @SafeParceled(1)
    private final int versionCode = 4;
    @SafeParceled(3)
    public String accountName;
    @SafeParceled(4)
    public Bundle extras;
    @SafeParceled(9)
    public String consent;
    @SafeParceled(15)
    public String accountType;

    public Account getAccount() {
        return new Account(accountName, accountType);
    }

    public static final Creator<TokenRequest> CREATOR = new AutoCreator<TokenRequest>(TokenRequest.class);
}
