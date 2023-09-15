/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * An abstract class representing FIDO2 request options.
 */
@PublicApi
public abstract class RequestOptions extends AutoSafeParcelable {
    @NonNull
    public abstract byte[] getChallenge();
    @Nullable
    public abstract Double getTimeoutSeconds();
    @Nullable
    public abstract Integer getRequestId();
    @Nullable
    public abstract TokenBinding getTokenBinding();
    @Nullable
    public abstract AuthenticationExtensions getAuthenticationExtensions();

    /**
     * Serializes the {@link RequestOptions} to bytes. Use deserializeFromBytes(byte[]) to deserialize.
     */
    @NonNull
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }
}
