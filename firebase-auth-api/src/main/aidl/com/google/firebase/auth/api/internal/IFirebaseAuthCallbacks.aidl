package com.google.firebase.auth.api.internal;

import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.api.internal.CreateAuthUriResponse;
import com.google.firebase.auth.api.internal.GetAccountInfoUser;
import com.google.firebase.auth.api.internal.GetTokenResponse;
import com.google.firebase.auth.api.internal.ResetPasswordResponse;

interface IFirebaseAuthCallbacks {
    void onGetTokenResponse(in GetTokenResponse response) = 0;
    void onGetTokenResponseAndUser(in GetTokenResponse response, in GetAccountInfoUser user) = 1;
    void onCreateAuthUriResponse(in CreateAuthUriResponse response) = 2;
    void onResetPasswordResponse(in ResetPasswordResponse response) = 3;
    void onFailure(in Status status) = 4;
    void onDeleteAccountResponse() = 5;
    void onEmailVerificationResponse() = 6;

    void onSendVerificationCodeResponse(String sessionInfo) = 8;
    void onVerificationCompletedResponse(in PhoneAuthCredential credential) = 9;
    void onVerificationAutoTimeOut(String sessionInfo) = 10;
}
