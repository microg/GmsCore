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

package org.microg.gms.people;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.common.internal.ICancelToken;
import com.google.android.gms.people.internal.IPeopleCallbacks;
import com.google.android.gms.people.internal.IPeopleService;
import com.google.android.gms.people.model.AccountMetadata;

import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.GooglePackagePermission;
import org.microg.gms.common.NonCancelToken;
import org.microg.gms.common.PackageUtils;

import java.io.File;

public class PeopleServiceImpl extends IPeopleService.Stub {
    private static final String TAG = "GmsPeopleSvcImpl";
    private final Context context;

    public PeopleServiceImpl(Context context) {
        this.context = context;
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void loadOwners(final IPeopleCallbacks callbacks, boolean var2, boolean var3, final String accountName, String var5, int sortOrder) {
        Log.d(TAG, "loadOwners: " + var2 + ", " + var3 + ", " + accountName + ", " + var5 + ", " + sortOrder);
        if (context.checkCallingPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.OWNER);
        }
        AccountManager accountManager = AccountManager.get(context);
        Bundle accountMetadata = new Bundle();
        String accountType = AuthConstants.DEFAULT_ACCOUNT_TYPE;
        for (Account account : accountManager.getAccountsByType(accountType)) {
            if (accountName == null || account.name.equals(accountName)) {
                accountMetadata.putParcelable(account.name, new AccountMetadata());
            }
        }
        Bundle extras = new Bundle();
        extras.putBundle("account_metadata", accountMetadata);
        try {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            DataHolder dataHolder = new DataHolder(databaseHelper.getOwners(), 0, extras);
            Log.d(TAG, "loadOwners[result]: " + dataHolder);
            callbacks.onDataHolder(0, extras, dataHolder);
            databaseHelper.close();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void loadPeopleForAggregation(IPeopleCallbacks callbacks, String account, String var3, String filter, int var5, boolean var6, int var7, int var8, String var9, boolean var10, int var11, int var12) throws RemoteException {
        Log.d(TAG, "loadPeopleForAggregation: " + account + ", " + var3 + ", " + filter + ", " + var5 + ", " + var6 + ", " + var7 + ", " + var8 + ", " + var9 + ", " + var10 + ", " + var11 + ", " + var12);
        callbacks.onDataHolder(0, new Bundle(), null);
    }

    @Override
    public Bundle registerDataChangedListener(IPeopleCallbacks callbacks, boolean register, String var3, String var4, int scopes) throws RemoteException {
        Log.d(TAG, "registerDataChangedListener: " + register + ", " + var3 + ", " + var4 + ", " + scopes);
        callbacks.onDataHolder(0, new Bundle(), null);
        return null;
    }

    @Override
    public void loadCircles(IPeopleCallbacks callbacks, String account, String pageGaiaId, String circleId, int type, String var6, boolean var7) throws RemoteException {
        Log.d(TAG, "loadCircles: " + account + ", " + pageGaiaId + ", " + circleId + ", " + type + ", " + var6 + ", " + var7);
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.PEOPLE);
        try {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            Cursor owner = databaseHelper.getOwner(account);
            int ownerId = -1;
            if (owner.moveToNext()) {
                ownerId = owner.getInt(0);
            }
            owner.close();
            Bundle extras = new Bundle();
            DataHolder dataHolder = new DataHolder(databaseHelper.getCircles(ownerId, circleId, type), 0, extras);
            callbacks.onDataHolder(0, new Bundle(), dataHolder);
            databaseHelper.close();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public Bundle requestSync(String account, String var2, long var3, boolean var5, boolean var6) throws RemoteException {
        Log.d(TAG, "requestSync: " + account + ", " + var2 + ", " + var3 + ", " + var5 + ", " + var6);
        return null;
    }

    @Override
    public ICancelToken loadOwnerAvatar(final IPeopleCallbacks callbacks, final String account, String pageId, int size, int flags) {
        Log.d(TAG, "loadOwnerAvatar: " + account + ", " + pageId + ", " + size + ", " + flags);
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.OWNER);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle extras = new Bundle();
                extras.putBoolean("rewindable", false);
                extras.putInt("width", 0);
                extras.putInt("height", 0);
                File avaterFile = PeopleManager.getOwnerAvatarFile(context, account, true);
                try {
                    ParcelFileDescriptor fileDescriptor = null;
                    if (avaterFile != null) {
                        fileDescriptor = ParcelFileDescriptor.open(avaterFile, ParcelFileDescriptor.MODE_READ_ONLY);
                    }
                    callbacks.onParcelFileDescriptor(0, extras, fileDescriptor, extras);
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
        });
        thread.start();
        return new ICancelToken.Stub() {
            @Override
            public void cancel() throws RemoteException {
                thread.interrupt();
            }
        };
    }

    @Override
    public ICancelToken loadAutocompleteList(IPeopleCallbacks callbacks, String account, String pageId, boolean directorySearch, String var5, String query, int autocompleteType, int var8, int numberOfResults, boolean var10) throws RemoteException {
        Log.d(TAG, "loadAutocompleteList: " + account + ", " + pageId + ", " + directorySearch + ", " + var5 + ", " + query + ", " + autocompleteType + ", " + var8 + ", " + numberOfResults + ", " + var10);
        callbacks.onDataHolder(0, new Bundle(), null);
        return new NonCancelToken();
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
