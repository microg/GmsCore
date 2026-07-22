/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Developers should implement this interface to provide options needed to create and initialize {@link CastContext}. The
 * implementation class must have a constructor without argument. The SDK will call that constructor to instantiate a new
 * instance.
 */
public interface OptionsProvider {
    /**
     * Provides a list of custom {@link SessionProvider} instances for non-Cast devices. This is optional.
     *
     * @param appContext The application {@link Context}.
     * @return the list of {@link SessionProvider} instances, may be {@code null}.
     */
    @Nullable
    List<SessionProvider> getAdditionalSessionProviders(@NonNull Context appContext);

    /**
     * Provides {@link CastOptions}, which affects discovery and session management of a Cast device.
     *
     * @param appContext The application {@link Context}.
     * @return the {@link CastOptions}, must not be {@code null}.
     */
    @NonNull
    CastOptions getCastOptions(@NonNull Context appContext);
}
