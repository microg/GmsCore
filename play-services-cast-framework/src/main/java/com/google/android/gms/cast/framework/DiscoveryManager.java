/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework;

import android.content.Context;
import android.os.RemoteException;
import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.dynamic.IObjectWrapper;

class DiscoveryManager {
    private Context context;
    private IDiscoveryManager delegate;

    public DiscoveryManager(Context context, IDiscoveryManager delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public IObjectWrapper getWrappedThis() {
        try {
            return delegate.getWrappedThis();
        } catch (RemoteException e) {
            return null;
        }
    }
}
