/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard;

import android.content.Context;

import org.microg.gms.droidguard.DroidGuardClientImpl;

public class DroidGuard {
    public static DroidGuardClient getClient(Context context) {
        return new DroidGuardClientImpl(context);
    }
    public static DroidGuardClient getClient(Context context, String packageName) {
        return new DroidGuardClientImpl(context, packageName);
    }
}
