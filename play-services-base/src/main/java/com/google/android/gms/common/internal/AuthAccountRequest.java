/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class AuthAccountRequest extends AutoSafeParcelable {
    public static final Creator<AuthAccountRequest> CREATOR = new AutoCreator<>(AuthAccountRequest.class);
}
