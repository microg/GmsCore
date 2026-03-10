package com.google.android.gms.recaptcha.internal;

import com.google.android.gms.common.api.Status;

interface ICloseCallback {
    oneway void onClosed(in Status status, boolean closed) = 0;
}
