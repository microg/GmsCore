/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import org.microg.gms.identitycredentials.IdentityCredentialClientImpl;

/**
 * Entry point for Identity Credential API.
 */
public final class IdentityCredentialManager {
    /**
     * Creates a new instance of {@link IdentityCredentialClient}.
     *
     * @param activity the activity that is using this client.
     */
    @NonNull
    public static IdentityCredentialClient getClient(@NonNull Activity activity) {
        return new IdentityCredentialClientImpl(activity);
    }

    /**
     * Creates a new instance of {@link IdentityCredentialClient}.
     *
     * @param context the context that is using this client.
     */
    @NonNull
    public static IdentityCredentialClient getClient(@NonNull Context context) {
        return new IdentityCredentialClientImpl(context);
    }

    private IdentityCredentialManager() {
    }
}
