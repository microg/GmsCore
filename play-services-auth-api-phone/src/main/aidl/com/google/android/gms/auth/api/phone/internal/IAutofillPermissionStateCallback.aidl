package com.google.android.gms.auth.api.phone.internal;

import com.google.android.gms.common.api.Status;

interface IAutofillPermissionStateCallback {
    void onCheckPermissionStateResult(in Status status, int result) = 0;
}
