/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.signin;

import android.content.Context;
import android.os.IBinder;
import com.google.android.gms.auth.api.signin.internal.ISignInService;
import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;

public class GoogleSignInGmsClientImpl extends GmsClient<ISignInService> {
    public GoogleSignInGmsClientImpl(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.AUTH_GOOGLE_SIGN_IN.ACTION);
        serviceId = GmsService.AUTH_GOOGLE_SIGN_IN.SERVICE_ID;
    }

    @Override
    protected ISignInService interfaceFromBinder(IBinder binder) {
        return ISignInService.Stub.asInterface(binder);
    }
}
