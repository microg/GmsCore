/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework;

import android.content.Context;
import android.os.IBinder;
import org.microg.gms.cast.ISessionProviderImpl;

/**
 * An abstract base class for performing session construction. The SDK uses a subclass of {@link SessionProvider} to
 * construct {@link CastSession} internally. If your app wants to support other types of {@link Session} then you should subclass this
 * class. Subclasses must implement {@link #createSession(String)} and {@link #isSessionRecoverable()}, which will be called by
 * the Cast SDK during the lifecycle of the session. All methods must be called from the main thread.
 */
public abstract class SessionProvider {
    private Context context;
    private String category;
    private ISessionProvider bindable = new ISessionProviderImpl(this);

    /**
     * Constructs a {@link SessionProvider} with a category string. The category uniquely identifies a {@link Session} created by this
     * provider.
     *
     * @param applicationContext The application Context of the calling app.
     * @param category           The category string used to create {@link Session}.
     */
    protected SessionProvider(Context applicationContext, String category) {
        this.context = applicationContext;
        this.category = category;
    }

    /**
     * Constructs a new {@link Session}. This method is called by the SDK to create a new session.
     */
    public abstract Session createSession(String sessionId);

    /**
     * Returns the category string for this {@link SessionProvider}.
     */
    public final String getCategory() {
        return category;
    }

    /**
     * Returns the application {@link Context} used to construct this instance.
     */
    public final Context getContext() {
        return context;
    }

    /**
     * Returns {@code true} if a previously constructed session can be resumed. Subclasses should check any persisted information
     * about the previous session, such as a session ID, and return true only if it is possible to resume that session. This
     * method is called by the SDK when it tries to resume a previously saved session.
     */
    public abstract boolean isSessionRecoverable();

    public IBinder asBinder() {
        return bindable.asBinder();
    }
}
