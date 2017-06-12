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

package org.microg.gms.icing;

import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.appdatasearch.CorpusStatus;
import com.google.android.gms.appdatasearch.PIMEUpdateResponse;
import com.google.android.gms.appdatasearch.RequestIndexingSpecification;
import com.google.android.gms.appdatasearch.SuggestSpecification;
import com.google.android.gms.appdatasearch.SuggestionResults;
import com.google.android.gms.appdatasearch.internal.IAppDataSearch;

public class AppDataSearchImpl extends IAppDataSearch.Stub {
    private static final String TAG = "GmsIcingAppDataImpl";

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }

    @Override
    public SuggestionResults getSuggestions(String var1, String packageName, String[] accounts, int maxNum, SuggestSpecification specs) throws RemoteException {
        return new SuggestionResults("Unknown error");
    }

    @Override
    public boolean requestIndexing(String packageName, String accountName, long l, RequestIndexingSpecification specs) throws RemoteException {
        Log.d(TAG, "requestIndexing: " + accountName + " @ " + packageName + ", " + l);
        return true;
    }

    @Override
    public CorpusStatus getStatus(String packageName, String accountName) throws RemoteException {
        Log.d(TAG, "getStatus: " + accountName + " @ " + packageName);
        CorpusStatus status = new CorpusStatus();
        status.found = true;
        return status;
    }

    @Override
    public PIMEUpdateResponse requestPIMEUpdate(String s1, String s2, int i, byte[] bs) throws RemoteException {
        Log.d(TAG, "requestPIMEUpdate: " + s1 + ", " + s2 + ", " + i + ", " + (bs == null ? "null" : new String(bs)));
        return new PIMEUpdateResponse();
    }
}
