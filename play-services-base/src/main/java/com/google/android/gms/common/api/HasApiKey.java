/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.api;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.internal.ApiKey;
import org.microg.gms.common.Hide;

public interface HasApiKey<O extends Api.ApiOptions> {
    @NonNull
    @Hide
    ApiKey<O> getApiKey();
}
