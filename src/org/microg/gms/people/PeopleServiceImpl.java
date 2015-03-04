/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.people;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.people.internal.IPeopleCallbacks;
import com.google.android.gms.people.internal.IPeopleService;
import com.google.android.gms.people.model.AccountMetadata;

public class PeopleServiceImpl extends IPeopleService.Stub {
    private static final String TAG = "GmsPeopleSvcImpl";
    private Context context;

    public PeopleServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public void loadOwners(final IPeopleCallbacks callbacks, boolean var2, boolean var3, final String accountName, String var5, int sortOrder) {
        Log.d(TAG, "loadOwners: " + var2 + ", " + var3 + ", " + accountName + ", " + var5 + ", " + sortOrder);
        new Thread(new Runnable() {
            @Override
            public void run() {
                AccountManager accountManager = AccountManager.get(context);
                Bundle result = new Bundle();
                for (Account account : accountManager.getAccountsByType("com.google")) {
                    if (accountName == null || account.name.equals(accountName)) {
                        result.putParcelable(account.name, new AccountMetadata());
                    }
                }
                try {
                    callbacks.onDataHolders(0, result, new DataHolder[0]);
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        }).start();
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
