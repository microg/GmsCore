package com.google.android.gms.common.internal;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ICertData {
    IObjectWrapper getWrappedBytes();
    int remoteHashCode();
}