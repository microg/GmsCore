package com.google.firebase.auth.api.internal;

import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.api.internal.CreateAuthUriResponse;
import com.google.firebase.auth.api.internal.GetAccountInfoUser;
import com.google.firebase.auth.api.internal.GetTokenResponse;
import com.google.firebase.auth.api.internal.ResetPasswordResponse;

interface IFirebaseAuthCallbacks {
    oneway void onGetTokenResponse(in GetTokenResponse response) = 0;
    oneway void onGetTokenResponseAndUser(in GetTokenResponse response, in GetAccountInfoUser user) = 1;
    oneway void onCreateAuthUriResponse(in CreateAuthUriResponse response) = 2;
    oneway void onResetPasswordResponse(in ResetPasswordResponse response) = 3;
    oneway void onFailure(in Status status) = 4;
    oneway void onDeleteAccountResponse() = 5;
    oneway void onEmailVerificationResponse() = 6;
    //oneway void onSetAccountInfo(String s) = 7
    oneway void onSendVerificationCodeResponse(String sessionInfo) = 8;
    oneway void onVerificationCompletedResponse(in PhoneAuthCredential credential) = 9;
    oneway void onVerificationAutoTimeOut(String sessionInfo) = 10;
}
