package com.google.android.gms.auth.api.signin.internal;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Status;

interface ISignInCallbacks {
    void onSignIn(in GoogleSignInAccount callbacks, in Status status) = 100;
    void onSignOut(in Status status) = 101;
    void onRevokeAccess(in Status status) = 102;
}