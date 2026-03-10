package com.google.android.gms.recaptcha.internal;

import com.google.android.gms.common.api.Status;

import com.google.android.gms.recaptcha.RecaptchaHandle;
import com.google.android.gms.recaptcha.internal.InitResults;

interface IInitCallback {
    oneway void onHandle(in Status status, in RecaptchaHandle handle) = 0;
    oneway void onResults(in Status status, in InitResults results) = 1;
}
