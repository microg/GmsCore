/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Api;

import java.util.Arrays;

public class SignInOptions implements Api.ApiOptions.Optional {
    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{SignInOptions.class});
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof SignInOptions;
    }
}
