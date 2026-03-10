/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.api.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Api;
import org.microg.gms.common.Hide;

import java.util.Objects;

@Hide
public class ApiKey<O extends Api.ApiOptions> {
    private final Api<O> api;
    @Nullable
    private final O options;
    @Nullable
    private final String attributionTag;

    private ApiKey(Api<O> api, @Nullable O options, @Nullable String attributionTag) {
        this.api = api;
        this.options = options;
        this.attributionTag = attributionTag;
    }

    @NonNull
    @Hide
    public static <O extends Api.ApiOptions> ApiKey<O> getSharedApiKey(@NonNull Api<O> api, @Nullable O o, @Nullable String attributionTag) {
        return new ApiKey<>(api, o, attributionTag);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ApiKey)) return false;

        ApiKey<?> apiKey = (ApiKey<?>) o;
        return Objects.equals(api, apiKey.api) && Objects.equals(options, apiKey.options) && Objects.equals(attributionTag, apiKey.attributionTag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{api, options, attributionTag});
    }
}
