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

package com.google.firebase.database.connection.idl;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;

import java.util.List;

public class IPersistentConnectionImpl extends IPersistentConnection.Stub {
    private static final String TAG = "GmsFirebaseDbConImpl";

    @Override
    public void setup(ConnectionConfig var1, IConnectionAuthTokenProvider var2, IObjectWrapper var3, IPersistentConnectionDelegate var4) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setup");

    }

    @Override
    public void initialize() throws RemoteException {
        Log.d(TAG, "unimplemented Method: initialize");

    }

    @Override
    public void shutdown() throws RemoteException {
        Log.d(TAG, "unimplemented Method: shutdown");

    }

    @Override
    public void refreshAuthToken() throws RemoteException {
        Log.d(TAG, "unimplemented Method: refreshAuthToken");

    }

    @Override
    public void listen(List<String> var1, IObjectWrapper var2, IListenHashProvider var3, long var4, IRequestResultCallback var6) throws RemoteException {
        Log.d(TAG, "unimplemented Method: listen");

    }

    @Override
    public void unlisten(List<String> var1, IObjectWrapper var2) throws RemoteException {
        Log.d(TAG, "unimplemented Method: unlisten");

    }

    @Override
    public void purgeOutstandingWrites() throws RemoteException {
        Log.d(TAG, "unimplemented Method: purgeOutstandingWrites");

    }

    @Override
    public void put(List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) throws RemoteException {
        Log.d(TAG, "unimplemented Method: put");

    }

    @Override
    public void compareAndPut(List<String> var1, IObjectWrapper var2, String var3, IRequestResultCallback var4) throws RemoteException {
        Log.d(TAG, "unimplemented Method: compareAndPut");

    }

    @Override
    public void merge(List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) throws RemoteException {
        Log.d(TAG, "unimplemented Method: merge");

    }

    @Override
    public void onDisconnectPut(List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onDisconnectPut");

    }

    @Override
    public void onDisconnectMerge(List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onDisconnectMerge");

    }

    @Override
    public void onDisconnectCancel(List<String> var1, IRequestResultCallback var2) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onDisconnectCancel");

    }

    @Override
    public void interrupt(String var1) throws RemoteException {
        Log.d(TAG, "unimplemented Method: interrupt");

    }

    @Override
    public void resume(String var1) throws RemoteException {
        Log.d(TAG, "unimplemented Method: resume");

    }

    @Override
    public boolean isInterrupted(String var1) throws RemoteException {
        Log.d(TAG, "unimplemented Method: isInterrupted");
        return false;
    }
}
