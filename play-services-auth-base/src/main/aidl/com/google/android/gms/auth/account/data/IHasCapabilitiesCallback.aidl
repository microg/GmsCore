package com.google.android.gms.auth.account.data;

import com.google.android.gms.common.api.Status;

interface IHasCapabilitiesCallback {
    void onHasCapabilities(in Status status, int mode) = 1;
}