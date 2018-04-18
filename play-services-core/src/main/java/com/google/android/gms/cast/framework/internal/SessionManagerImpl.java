/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.cast.framework.internal;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.cast.framework.ICastStateListener;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.ISessionManagerListener;
import com.google.android.gms.cast.framework.internal.SessionImpl;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.Set;
import java.util.HashSet;

public class SessionManagerImpl extends ISessionManager.Stub {
    private static final String TAG = SessionManagerImpl.class.getSimpleName();

    private Set sessionManagerListeners = new HashSet();
    private Set castStateListeners = new HashSet();

    private SessionImpl currentSession;

    @Override
    public IObjectWrapper getWrappedCurrentSession() throws RemoteException {
        Log.d(TAG, "unimplemented Method: getWrappedCurrentSession");
        return ObjectWrapper.wrap(this.currentSession);
    }

    @Override
    public void endCurrentSession(boolean b, boolean stopCasting) throws RemoteException {
        Log.d(TAG, "unimplemented Method: endCurrentSession");
    }

    @Override
    public void addSessionManagerListener(ISessionManagerListener listener) {
        Log.d(TAG, "unimplemented Method: addSessionManagerListener");
        this.sessionManagerListeners.add(listener);
    }

    @Override
    public void removeSessionManagerListener(ISessionManagerListener listener) {
        Log.d(TAG, "unimplemented Method: removeSessionManagerListener");
        this.sessionManagerListeners.remove(listener);
    }

    @Override
    public void addCastStateListener(ICastStateListener listener) {
        Log.d(TAG, "unimplemented Method: addCastStateListener");
        this.castStateListeners.add(listener);
    }

    @Override
    public void removeCastStateListener(ICastStateListener listener) {
        Log.d(TAG, "unimplemented Method: removeCastStateListener");
        this.castStateListeners.remove(listener);
    }

    @Override
    public IObjectWrapper getWrappedThis() throws RemoteException {
        return ObjectWrapper.wrap(this);
    }
}
