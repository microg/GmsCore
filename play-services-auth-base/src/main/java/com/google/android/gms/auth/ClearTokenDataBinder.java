/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

import com.google.android.auth.IAuthManagerService;

public class ClearTokenDataBinder implements DataBinder<Void> {

    private final String token;
    private final Bundle extras;

    public ClearTokenDataBinder(String token, Bundle extras) {
        this.token = token;
        this.extras = extras;
    }

    @Override
    public Void getBinderData(IBinder iBinder) throws Exception {
        IAuthManagerService service;
        if (iBinder == null) {
            service = null;
        } else {
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.google.android.auth.IAuthManagerService");
            if (queryLocalInterface instanceof IAuthManagerService) {
                service = (IAuthManagerService) queryLocalInterface;
            } else {
                service = IAuthManagerService.Stub.asInterface(iBinder);
            }
        }
        if (service == null) {
            return null;
        }
        service.clearToken(token, extras);
        return Void.TYPE.newInstance();
    }
}
