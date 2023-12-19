package com.google.android.gms.potokens.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.potokens.PoToken;

interface  ITokenCallbacks{
    void responseToken(in Status status,in PoToken token)=1;
}