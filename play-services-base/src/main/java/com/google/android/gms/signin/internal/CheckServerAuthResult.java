/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class CheckServerAuthResult extends AutoSafeParcelable {
    public static final Creator<CheckServerAuthResult> CREATOR = new AutoCreator<>(CheckServerAuthResult.class);
}
