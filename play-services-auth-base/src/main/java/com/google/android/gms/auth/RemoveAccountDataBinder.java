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

public class RemoveAccountDataBinder implements DataBinder<Bundle> {

    private final Account account;

    public RemoveAccountDataBinder(Account account) {
        this.account = account;
    }

    @Override
    public Bundle getBinderData(IBinder iBinder) throws Exception {
        IAuthManagerService service;
        if (iBinder == null) {
            service = null;
        } else {
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.google.android.auth.IAuthManagerService");
            if (queryLocalInterface instanceof IAuthManagerService) {
                service = (IAuthManagerService) queryLocalInterface;
            } else {
                service = IAuthManagerService.Stub.asInterface(iBinder);
            }
        }
        if (service == null) {
            return null;
        }
        return service.removeAccount(account);
    }
}
