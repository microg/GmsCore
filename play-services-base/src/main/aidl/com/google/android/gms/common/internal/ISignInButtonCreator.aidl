package com.google.android.gms.common.internal;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISignInButtonCreator {
    IObjectWrapper createSignInButton(IObjectWrapper context, int size, int color); // returns View
}
