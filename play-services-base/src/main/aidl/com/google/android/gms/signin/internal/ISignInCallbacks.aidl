package com.google.android.gms.signin.internal;

import com.google.android.gms.signin.internal.SignInResponse;

interface ISignInCallbacks {
    void onSignIn(in SignInResponse response) = 7;
}