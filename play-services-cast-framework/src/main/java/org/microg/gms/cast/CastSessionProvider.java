/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.content.Context;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionProvider;

public class CastSessionProvider extends SessionProvider {
    private CastOptions castOptions;

    public CastSessionProvider(Context applicationContext, CastOptions castOptions) {
        super(applicationContext, castOptions.getSupportedNamespaces().isEmpty() ? CastMediaControlIntent.categoryForCast(castOptions.getReceiverApplicationId()) : CastMediaControlIntent.categoryForCast(castOptions.getReceiverApplicationId(), castOptions.getSupportedNamespaces()));
        this.castOptions = castOptions;
    }

    @Override
    public Session createSession(String sessionId) {
        return null;
    }

    @Override
    public boolean isSessionRecoverable() {
        return false;
    }
}
