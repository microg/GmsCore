/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import android.app.PendingIntent;

import org.microg.safeparcel.AutoSafeParcelable;

public class BeginSignInResult extends AutoSafeParcelable {
    @Field(1)
    public PendingIntent pendingIntent;

    public static final Creator<BeginSignInResult> CREATOR = findCreator(BeginSignInResult.class);
}
