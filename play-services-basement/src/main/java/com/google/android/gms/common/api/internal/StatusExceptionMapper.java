/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.api.internal;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Status;
import org.microg.gms.common.Hide;

@Hide
public interface StatusExceptionMapper {
    @NonNull
    Exception getException(@NonNull Status status);
}
