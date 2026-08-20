/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;

import androidx.annotation.NonNull;

import org.microg.gms.common.PackageUtils;

public final class CallerVerifier {
    static final String MESSAGES_PACKAGE = "com.google.android.apps.messaging";

    /* Google application signing key used by Google Messages releases. */
    private static final String GOOGLE_APPLICATION_SHA1 =
            "24bb24c05e47e0aefa68a58a766179d9b613a600";

    private final Context context;

    public CallerVerifier(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isAllowedClient(String requestedPackage) {
        if (!MESSAGES_PACKAGE.equals(requestedPackage)) {
            return false;
        }

        try {
            PackageUtils.checkPackageUid(
                    context,
                    requestedPackage,
                    Binder.getCallingUid());
        } catch (SecurityException | IllegalArgumentException exception) {
            return false;
        }

        String currentDigest = PackageUtils.firstSignatureDigest(
                context,
                requestedPackage,
                true);
        String historicalDigest = PackageUtils.firstSignatureDigest(
                context,
                requestedPackage,
                false);

        return GOOGLE_APPLICATION_SHA1.equalsIgnoreCase(currentDigest)
                || GOOGLE_APPLICATION_SHA1.equalsIgnoreCase(historicalDigest);
    }
}