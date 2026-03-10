/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework;

import android.content.Context;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

public class SessionManager {
    private Context context;
    private ISessionManager delegate;

    SessionManager(Context context, ISessionManager delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    /**
     * Ends the current session.
     *
     * @param stopCasting Should the receiver application be stopped when ending the current Session.
     * @throws IllegalStateException If this method is not called on the main thread.
     */
    public void endCurrentSession(boolean stopCasting) {
        try {
            delegate.endCurrentSession(true, stopCasting);
        } catch (RemoteException e) {
            // Ignore
        }
    }

    /**
     * Returns the current session if it is an instance of {@link CastSession}, otherwise returns {@code null}.
     *
     * @throws IllegalStateException If this method is not called on the main thread.
     */
    public CastSession getCurrentCastSession() {
        Session currentSession = getCurrentSession();
        if (currentSession instanceof CastSession) {
            return (CastSession) currentSession;
        }
        return null;
    }

    /**
     * Returns the currently active session. Returns {@code null} if no session is active.
     *
     * @throws IllegalStateException If this method is not called on the main thread.
     */
    public Session getCurrentSession() {
        try {
            return ObjectWrapper.unwrapTyped(delegate.getWrappedCurrentSession(), Session.class);
        } catch (RemoteException e) {
            return null;
        }
    }

    IObjectWrapper getWrappedThis() {
        try {
            return delegate.getWrappedThis();
        } catch (RemoteException e) {
            return null;
        }
    }
}
