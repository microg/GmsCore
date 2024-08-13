/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.accounts.Account;
import android.os.IBinder;
import android.os.IInterface;

import com.google.android.auth.IAuthManagerService;

import java.util.List;

public class ChangeEventDataBinder implements DataBinder<List<AccountChangeEvent>> {

    private final String accountName;
    private final int since;

    public ChangeEventDataBinder(String accountName, int since) {
        this.accountName = accountName;
        this.since = since;
    }

    @Override
    public List<AccountChangeEvent> getBinderData(IBinder iBinder) throws Exception {
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
        AccountChangeEventsRequest request = new AccountChangeEventsRequest(1, since, accountName, new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE));
        AccountChangeEventsResponse changeEvents = service.getChangeEvents(request);
        return changeEvents.getEvents();
    }
}
