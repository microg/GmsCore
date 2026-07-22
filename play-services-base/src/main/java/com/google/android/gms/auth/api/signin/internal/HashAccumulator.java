/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.signin.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.common.Hide;

/**
 * We keep this mostly for documentary purposes and drop-in compatibility. Usage is highly discouraged.
 *
 * @deprecated Directly compute hash code when needed.
 */
@Hide
@Deprecated
public class HashAccumulator {
    private int hash = 1;

    @NonNull
    @Hide
    @Deprecated
    public HashAccumulator addBoolean(boolean bool) {
        hash = (hash * 31) + (bool ? 1 : 0);
        return this;
    }

    @NonNull
    @Deprecated
    public HashAccumulator addObject(@Nullable Object object) {
        hash = (object == null ? 0 : object.hashCode()) + hash * 31;
        return this;
    }

    @Deprecated
    public int hash() {
        return hash;
    }
}
