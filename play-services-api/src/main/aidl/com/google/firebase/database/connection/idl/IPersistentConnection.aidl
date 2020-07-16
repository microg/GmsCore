package com.google.firebase.database.connection.idl;

import com.google.android.gms.dynamic.IObjectWrapper;

import com.google.firebase.database.connection.idl.ConnectionConfig;
import com.google.firebase.database.connection.idl.IConnectionAuthTokenProvider;
import com.google.firebase.database.connection.idl.IListenHashProvider;
import com.google.firebase.database.connection.idl.IPersistentConnectionDelegate;
import com.google.firebase.database.connection.idl.IRequestResultCallback;


interface IPersistentConnection {
    void setup(in ConnectionConfig var1, IConnectionAuthTokenProvider var2, IObjectWrapper var3, IPersistentConnectionDelegate var4) = 0;

    void initialize() = 1;

    void shutdown() = 2;

    void refreshAuthToken() = 3;

    void listen(in List<String> var1, IObjectWrapper var2, IListenHashProvider var3, long var4, IRequestResultCallback var6) = 4;

    void unlisten(in List<String> var1, IObjectWrapper var2) = 5;

    void purgeOutstandingWrites() = 6;

    void put(in List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) = 7;

    void compareAndPut(in List<String> var1, IObjectWrapper var2, String var3, IRequestResultCallback var4) = 8;

    void merge(in List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) = 9;

    void onDisconnectPut(in List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) = 10;

    void onDisconnectMerge(in List<String> var1, IObjectWrapper var2, IRequestResultCallback var3) = 11;

    void onDisconnectCancel(in List<String> var1, IRequestResultCallback var2) = 12;

    void interrupt(String var1) = 13;

    void resume(String var1) = 14;

    boolean isInterrupted(String var1) = 15;
}
