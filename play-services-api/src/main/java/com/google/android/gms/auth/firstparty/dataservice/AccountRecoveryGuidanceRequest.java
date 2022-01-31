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

import org.microg.gms.auth.AuthConstants;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AccountRecoveryGuidanceRequest extends AutoSafeParcelable {

    @SafeParceled(1)
    private final int versionCode = 1;
    @SafeParceled(2)
    @Deprecated
    public final String accountName;
    @SafeParceled(3)
    public final Account account;

    public AccountRecoveryGuidanceRequest(String accountName) {
        this.accountName = accountName;
        this.account = new Account(accountName, AuthConstants.DEFAULT_ACCOUNT_TYPE);
    }

    public AccountRecoveryGuidanceRequest(Account account) {
        this.accountName = account.name;
        this.account = account;
    }

    public static final Creator<AccountRecoveryGuidanceRequest> CREATOR = new AutoCreator<AccountRecoveryGuidanceRequest>(AccountRecoveryGuidanceRequest.class);
}
