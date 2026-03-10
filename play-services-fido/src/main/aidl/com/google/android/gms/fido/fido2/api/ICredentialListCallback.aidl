package com.google.android.gms.fido.fido2.api;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.fido.fido2.api.common.FidoCredentialDetails;

interface ICredentialListCallback {
    void onCredentialList(in List<FidoCredentialDetails> value) = 0;
    void onError(in Status status) = 1;
}
