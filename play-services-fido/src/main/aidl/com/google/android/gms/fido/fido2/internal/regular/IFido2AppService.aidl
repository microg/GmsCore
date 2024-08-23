package com.google.android.gms.fido.fido2.internal.regular;

import com.google.android.gms.fido.fido2.internal.regular.IFido2AppCallbacks;
import com.google.android.gms.fido.fido2.api.IBooleanCallback;
import com.google.android.gms.fido.fido2.api.ICredentialListCallback;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;

interface IFido2AppService {
    void getRegisterPendingIntent(IFido2AppCallbacks callbacks, in PublicKeyCredentialCreationOptions options) = 0;
    void getSignPendingIntent(IFido2AppCallbacks callbacks, in PublicKeyCredentialRequestOptions options) = 1;
    void isUserVerifyingPlatformAuthenticatorAvailable(IBooleanCallback callbacks) = 2;
}
