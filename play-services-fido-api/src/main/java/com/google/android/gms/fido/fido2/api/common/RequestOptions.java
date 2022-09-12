/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelUtil;

/**
 * An abstract class representing FIDO2 request options.
 */
@PublicApi
public abstract class RequestOptions extends AutoSafeParcelable {
    public abstract byte[] getChallenge();
    public abstract Double getTimeoutSeconds();
    public abstract Integer getRequestId();
    public abstract TokenBinding getTokenBinding();
    public abstract AuthenticationExtensions getAuthenticationExtensions();

    /**
     * Serializes the {@link RequestOptions} to bytes. Use deserializeFromBytes(byte[]) to deserialize.
     */
    public byte[] serializeToBytes() {
        return SafeParcelUtil.asByteArray(this);
    }
}
