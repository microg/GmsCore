package com.google.android.gms.auth.api.credentials.internal;

import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.internal.ICredentialsCallbacks;
import com.google.android.gms.auth.api.credentials.internal.DeleteRequest;
import com.google.android.gms.auth.api.credentials.internal.GeneratePasswordRequest;
import com.google.android.gms.auth.api.credentials.internal.SaveRequest;

interface ICredentialsService {
    void request(ICredentialsCallbacks callbacks, in CredentialRequest request) = 0;
    void save(ICredentialsCallbacks callbacks, in SaveRequest request) = 1;
    void delete(ICredentialsCallbacks callbacks, in DeleteRequest request) = 2;
    void disableAutoSignIn(ICredentialsCallbacks callbacks) = 3;
    void generatePassword(ICredentialsCallbacks callbacks, in GeneratePasswordRequest request) = 4;
}