/*
 * Copyright 2013-2015 microG Project Team
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

package org.microg.gms.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.R;

public class AccountContentProvider extends ContentProvider {
    private static final String TAG = "GmsAuthProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        // FIXME: Restrict access!
        if ("get_accounts".equals(method) && getContext().getString(R.string.google_account_type).equals(arg)) {
            Bundle result = new Bundle();
            result.putParcelableArray("accounts", AccountManager.get(getContext()).getAccountsByType(arg));
            return result;
        } else if ("clear_password".equals(method)) {
            Account a = extras.getParcelable("clear_password");
            AccountManager.get(getContext()).clearPassword(a);
            return null;
        }
        throw new UnsupportedOperationException(String.format("Unsupported method call %s(%s).", method, arg));
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
