/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

import com.google.android.auth.IAuthManagerService;

import java.util.Objects;

public class AccessAccountDataBinder implements DataBinder<Boolean> {

    private final String clientPackageName;

    public AccessAccountDataBinder(String clientPackageName) {
        this.clientPackageName = clientPackageName;
    }

    @Override
    public Boolean getBinderData(IBinder iBinder) throws Exception {
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
        Bundle bundle = service.requestGoogleAccountsAccess(clientPackageName);
        if (bundle == null) {
            return false;
        }
        return Objects.equals(bundle.getString("Error"), "OK");
    }
}
