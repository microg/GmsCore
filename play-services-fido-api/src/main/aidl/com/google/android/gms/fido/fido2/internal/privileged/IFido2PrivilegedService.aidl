package com.google.android.gms.fido.fido2.internal.privileged;

import com.google.android.gms.fido.fido2.internal.privileged.IFido2PrivilegedCallbacks;
import com.google.android.gms.fido.fido2.api.IBooleanCallback;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions;

interface IFido2PrivilegedService {
    void register(IFido2PrivilegedCallbacks callbacks, in BrowserPublicKeyCredentialCreationOptions options) = 0;
    void sign(IFido2PrivilegedCallbacks callbacks, in BrowserPublicKeyCredentialRequestOptions options) = 1;
    void isUserVerifyingPlatformAuthenticatorAvailable(IBooleanCallback callbacks) = 2;
}
