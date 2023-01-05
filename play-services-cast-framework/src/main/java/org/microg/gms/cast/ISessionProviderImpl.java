/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.os.RemoteException;
import com.google.android.gms.cast.framework.ISessionProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import org.microg.gms.common.Constants;

public class ISessionProviderImpl extends ISessionProvider.Stub {
    private SessionProvider delegate;

    public ISessionProviderImpl(SessionProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public IObjectWrapper getSession(String sessionId) throws RemoteException {
        return ObjectWrapper.wrap(delegate.createSession(sessionId));
    }

    @Override
    public boolean isSessionRecoverable() throws RemoteException {
        return delegate.isSessionRecoverable();
    }

    @Override
    public String getCategory() throws RemoteException {
        return delegate.getCategory();
    }

    @Override
    public int getSupportedVersion() throws RemoteException {
        return Constants.GMS_VERSION_CODE;
    }
}
