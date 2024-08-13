/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.accounts.Account;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

import com.google.android.auth.IAuthManagerService;

public class TokenDataBinder implements DataBinder<TokenData> {

    private final Account account;
    private final String scope;
    private final Bundle extras;

    public TokenDataBinder(Account account, String scope, Bundle extras) {
        this.account = account;
        this.scope = scope;
        this.extras = extras;
    }

    @Override
    public TokenData getBinderData(IBinder iBinder) throws Exception {
        IAuthManagerService managerService;
        if (iBinder == null) {
            managerService = null;
        } else {
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.google.android.auth.IAuthManagerService");
            if (queryLocalInterface instanceof IAuthManagerService) {
                managerService = (IAuthManagerService) queryLocalInterface;
            } else {
                managerService = IAuthManagerService.Stub.asInterface(iBinder);
            }
        }
        if (managerService == null) {
            return null;
        }
        Bundle bundle = managerService.getTokenWithAccount(account, scope, extras);
        return TokenData.getTokenData(bundle, "tokenDetails");
    }
}
