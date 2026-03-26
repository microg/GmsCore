/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import org.microg.gms.common.Hide;

public class SignInOptions implements Api.ApiOptions.Optional {
    @NonNull
    @Hide
    public static SignInOptions DEFAULT = new SignInOptions();
}
