package com.google.firebase.auth.api.internal;

import com.google.firebase.auth.api.internal.ApplyActionCodeAidlRequest;
import com.google.firebase.auth.api.internal.ChangeEmailAidlRequest;
import com.google.firebase.auth.api.internal.ChangePasswordAidlRequest;
import com.google.firebase.auth.api.internal.CheckActionCodeAidlRequest;
import com.google.firebase.auth.api.internal.ConfirmPasswordResetAidlRequest;
import com.google.firebase.auth.api.internal.CreateUserWithEmailAndPasswordAidlRequest;
import com.google.firebase.auth.api.internal.DeleteAidlRequest;
import com.google.firebase.auth.api.internal.FinalizeMfaEnrollmentAidlRequest;
import com.google.firebase.auth.api.internal.FinalizeMfaSignInAidlRequest;
import com.google.firebase.auth.api.internal.GetAccessTokenAidlRequest;
import com.google.firebase.auth.api.internal.GetProvidersForEmailAidlRequest;
import com.google.firebase.auth.api.internal.IFirebaseAuthCallbacks;
import com.google.firebase.auth.api.internal.LinkEmailAuthCredentialAidlRequest;
import com.google.firebase.auth.api.internal.LinkFederatedCredentialAidlRequest;
import com.google.firebase.auth.api.internal.LinkPhoneAuthCredentialAidlRequest;
import com.google.firebase.auth.api.internal.ReloadAidlRequest;
import com.google.firebase.auth.api.internal.SendEmailVerificationWithSettingsAidlRequest;
import com.google.firebase.auth.api.internal.SendGetOobConfirmationCodeEmailAidlRequest;
import com.google.firebase.auth.api.internal.SendVerificationCodeAidlRequest;
import com.google.firebase.auth.api.internal.SendVerificationCodeRequest;
import com.google.firebase.auth.api.internal.SetFirebaseUiVersionAidlRequest;
import com.google.firebase.auth.api.internal.SignInAnonymouslyAidlRequest;
import com.google.firebase.auth.api.internal.SignInWithCredentialAidlRequest;
import com.google.firebase.auth.api.internal.SignInWithCustomTokenAidlRequest;
import com.google.firebase.auth.api.internal.SignInWithEmailAndPasswordAidlRequest;
import com.google.firebase.auth.api.internal.SignInWithEmailLinkAidlRequest;
import com.google.firebase.auth.api.internal.SignInWithPhoneNumberAidlRequest;
import com.google.firebase.auth.api.internal.StartMfaPhoneNumberEnrollmentAidlRequest;
import com.google.firebase.auth.api.internal.StartMfaPhoneNumberSignInAidlRequest;
import com.google.firebase.auth.api.internal.UnenrollMfaAidlRequest;
import com.google.firebase.auth.api.internal.UnlinkEmailCredentialAidlRequest;
import com.google.firebase.auth.api.internal.UnlinkFederatedCredentialAidlRequest;
import com.google.firebase.auth.api.internal.UpdateProfileAidlRequest;
import com.google.firebase.auth.api.internal.VerifyAssertionRequest;
import com.google.firebase.auth.api.internal.VerifyBeforeUpdateEmailAidlRequest;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.UserProfileChangeRequest;

interface IFirebaseAuthService {
    void getAccessTokenCompat(String refreshToken, IFirebaseAuthCallbacks callbacks) = 0;
    void signInWithCustomTokenCompat(String token, IFirebaseAuthCallbacks callbacks) = 1;
    void signInWithCredentialCompat(in VerifyAssertionRequest verifyAssertionRequest, IFirebaseAuthCallbacks callbacks) = 2;
    void updateProfileCompat(String cachedState, in UserProfileChangeRequest userProfileChangeRequest, IFirebaseAuthCallbacks callbacks) = 3;
    void changeEmailCompat(String cachedState, String email, IFirebaseAuthCallbacks callbacks) = 4;
    void changePasswordCompat(String cachedState, String password, IFirebaseAuthCallbacks callbacks) = 5;
    void createUserWithEmailAndPasswordCompat(String email, String password, IFirebaseAuthCallbacks callbacks) = 6;
    void signInWithEmailAndPasswordCompat(String email, String password, IFirebaseAuthCallbacks callbacks) = 7;
    void getProvidersForEmailCompat(String email, IFirebaseAuthCallbacks callbacks) = 8;

    void linkEmailAuthCredentialCompat(String email, String password, String cachedState, IFirebaseAuthCallbacks callbacks) = 10;
    void linkFederatedCredentialCompat(String cachedState, in VerifyAssertionRequest verifyAssertionRequest, IFirebaseAuthCallbacks callbacks) = 11;
    void unlinkEmailCredentialCompat(String cachedState, IFirebaseAuthCallbacks callbacks) = 12;
    void unlinkFederatedCredentialCompat(String provider, String cachedState, IFirebaseAuthCallbacks callbacks) = 13;
    void reloadCompat(String cachedState, IFirebaseAuthCallbacks callbacks) = 14;
    void signInAnonymouslyCompat(IFirebaseAuthCallbacks callbacks) = 15;
    void deleteCompat(String cachedState, IFirebaseAuthCallbacks callbacks) = 16;
    void checkActionCodeCompat(String code, IFirebaseAuthCallbacks callbacks) = 18;
    void applyActionCodeCompat(String code, IFirebaseAuthCallbacks callbacks) = 19;
    void confirmPasswordResetCompat(String code, String newPassword, IFirebaseAuthCallbacks callbacks) = 20;
    void sendVerificationCodeCompat(in SendVerificationCodeRequest request, IFirebaseAuthCallbacks callbacks) = 21;
    void signInWithPhoneNumberCompat(in PhoneAuthCredential credential, IFirebaseAuthCallbacks callbacks) = 22;
    void linkPhoneAuthCredentialCompat(String cachedState, in PhoneAuthCredential credential, IFirebaseAuthCallbacks callbacks) = 23;

    void sendEmailVerificationCompat(String token, in ActionCodeSettings actionCodeSettings, IFirebaseAuthCallbacks callbacks) = 25;
    void setFirebaseUIVersionCompat(String firebaseUiVersion, IFirebaseAuthCallbacks callbacks) = 26;
    void sendGetOobConfirmationCodeEmailCompat(String email, in ActionCodeSettings actionCodeSettings, IFirebaseAuthCallbacks callbacks) = 27;
    void signInWithEmailLinkCompat(in EmailAuthCredential credential, IFirebaseAuthCallbacks callbacks) = 28;

    void getAccessToken(in GetAccessTokenAidlRequest request, IFirebaseAuthCallbacks callbacks) = 100;
    void signInWithCustomToken(in SignInWithCustomTokenAidlRequest request, IFirebaseAuthCallbacks callbacks) = 101;
    void signInWithCredential(in SignInWithCredentialAidlRequest request, IFirebaseAuthCallbacks callbacks) = 102;
    void updateProfile(in UpdateProfileAidlRequest request, IFirebaseAuthCallbacks callbacks) = 103;
    void changeEmail(in ChangeEmailAidlRequest request, IFirebaseAuthCallbacks callbacks) = 104;
    void changePassword(in ChangePasswordAidlRequest request, IFirebaseAuthCallbacks callbacks) = 105;
    void createUserWithEmailAndPassword(in CreateUserWithEmailAndPasswordAidlRequest request, IFirebaseAuthCallbacks callbacks) = 106;
    void signInWithEmailAndPassword(in SignInWithEmailAndPasswordAidlRequest request, IFirebaseAuthCallbacks callbacks) = 107;
    void getProvidersForEmail(in GetProvidersForEmailAidlRequest request, IFirebaseAuthCallbacks callbacks) = 108;

    void linkEmailAuthCredential(in LinkEmailAuthCredentialAidlRequest request, IFirebaseAuthCallbacks callbacks) = 110;
    void linkFederatedCredential(in LinkFederatedCredentialAidlRequest request, IFirebaseAuthCallbacks callbacks) = 111;
    void unlinkEmailCredential(in UnlinkEmailCredentialAidlRequest request, IFirebaseAuthCallbacks callbacks) = 112;
    void unlinkFederatedCredential(in UnlinkFederatedCredentialAidlRequest request, IFirebaseAuthCallbacks callbacks) = 113;
    void reload(in ReloadAidlRequest request, IFirebaseAuthCallbacks callbacks) = 114;
    void signInAnonymously(in SignInAnonymouslyAidlRequest request, IFirebaseAuthCallbacks callbacks) = 115;
    void delete(in DeleteAidlRequest request, IFirebaseAuthCallbacks callbacks) = 116;
    void checkActionCode(in CheckActionCodeAidlRequest request, IFirebaseAuthCallbacks callbacks) = 118;
    void applyActionCode(in ApplyActionCodeAidlRequest request, IFirebaseAuthCallbacks callbacks) = 119;
    void confirmPasswordReset(in ConfirmPasswordResetAidlRequest request, IFirebaseAuthCallbacks callbacks) = 120;
    void sendVerificationCode(in SendVerificationCodeAidlRequest request, IFirebaseAuthCallbacks callbacks) = 121;
    void signInWithPhoneNumber(in SignInWithPhoneNumberAidlRequest request, IFirebaseAuthCallbacks callbacks) = 122;
    void linkPhoneAuthCredential(in LinkPhoneAuthCredentialAidlRequest request, IFirebaseAuthCallbacks callbacks) = 123;

    void sendEmailVerification(in SendEmailVerificationWithSettingsAidlRequest request, IFirebaseAuthCallbacks callbacks) = 125;
    void setFirebaseUiVersion(in SetFirebaseUiVersionAidlRequest request, IFirebaseAuthCallbacks callbacks) = 126;
    void sendGetOobConfirmationCodeEmail(in SendGetOobConfirmationCodeEmailAidlRequest request, IFirebaseAuthCallbacks callbacks) = 127;
    void signInWithEmailLink(in SignInWithEmailLinkAidlRequest request, IFirebaseAuthCallbacks callbacks) = 128;

    void startMfaEnrollmentWithPhoneNumber(in StartMfaPhoneNumberEnrollmentAidlRequest request, IFirebaseAuthCallbacks callbacks) = 129;
    void unenrollMfa(in UnenrollMfaAidlRequest request, IFirebaseAuthCallbacks callbacks) = 130;
    void finalizeMfaEnrollment(in FinalizeMfaEnrollmentAidlRequest request, IFirebaseAuthCallbacks callbacks) = 131;
    void startMfaSignInWithPhoneNumber(in StartMfaPhoneNumberSignInAidlRequest request, IFirebaseAuthCallbacks callbacks) = 132;
    void finalizeMfaSignIn(in FinalizeMfaSignInAidlRequest request, IFirebaseAuthCallbacks callbacks) = 133;
    void verifyBeforeUpdateEmail(in VerifyBeforeUpdateEmailAidlRequest request, IFirebaseAuthCallbacks callbacks) = 134;
}
