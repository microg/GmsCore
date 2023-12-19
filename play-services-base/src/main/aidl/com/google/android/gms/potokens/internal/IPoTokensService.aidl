package com.google.android.gms.potokens.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.potokens.internal.ITokenCallbacks;

interface IPoTokensService{

    void responseStatus(IStatusCallback call,in int code)=1;

    void responseStatusToken(ITokenCallbacks call, in int code, in byte[] data)=2;
}