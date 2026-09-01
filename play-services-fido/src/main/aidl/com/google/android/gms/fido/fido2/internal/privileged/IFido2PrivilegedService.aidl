package com.google.android.gms.fido.fido2.internal.privileged;

import com.google.android.gms.fido.fido2.internal.privileged.IFido2PrivilegedCallbacks;
import com.google.android.gms.fido.fido2.api.IBooleanCallback;
import com.google.android.gms.fido.fido2.api.ICredentialListCallback;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions;

interface IFido2PrivilegedService {
    void getRegisterPendingIntent(IFido2PrivilegedCallbacks callbacks, in BrowserPublicKeyCredentialCreationOptions options) = 0;
    void getSignPendingIntent(IFido2PrivilegedCallbacks callbacks, in BrowserPublicKeyCredentialRequestOptions options) = 1;
    void isUserVerifyingPlatformAuthenticatorAvailable(IBooleanCallback callbacks) = 2;
    void getCredentialList(ICredentialListCallback callbacks, String rpId) = 3;
}
