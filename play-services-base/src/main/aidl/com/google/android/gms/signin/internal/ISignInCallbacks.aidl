package com.google.android.gms.signin.internal;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.signin.internal.AuthAccountResult;
import com.google.android.gms.signin.internal.RecordConsentByConsentResultResponse;
import com.google.android.gms.signin.internal.SignInResponse;

interface ISignInCallbacks {
    void onAuthAccount(in ConnectionResult connectionResult, in AuthAccountResult result) = 2;
    void onPutAccount(in Status status) = 3;
    void onRecordConsent(in Status status) = 5;
    void onCurrentAccount(in Status status, in GoogleSignInAccount account) = 6;
    void onSignIn(in SignInResponse response) = 7;
    void onRecordConsentByConsent(in RecordConsentByConsentResultResponse response) = 8;
}