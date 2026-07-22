/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.api;

import androidx.annotation.NonNull;
import com.google.android.gms.common.Feature;
import org.microg.gms.common.Hide;

/**
 * An API that requires an optional module.
 */
public interface OptionalModuleApi {
    @Hide
    @NonNull
    Feature[] getOptionalFeatures();
}
