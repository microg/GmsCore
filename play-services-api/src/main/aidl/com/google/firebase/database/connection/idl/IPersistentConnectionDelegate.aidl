package com.google.firebase.database.connection.idl;

import com.google.android.gms.dynamic.IObjectWrapper;

import com.google.firebase.database.connection.idl.RangeParcelable;

interface IPersistentConnectionDelegate {
    void zero(in List<String> var1, IObjectWrapper var2, boolean var3, long var4) = 0;

    void one(in List<String> var1, in List<RangeParcelable> var2, IObjectWrapper var3, long var4) = 1;

    void two() = 2;

    void onDisconnect() = 3;

    void four(boolean var1) = 4;

    void five(IObjectWrapper var1) = 5;
}