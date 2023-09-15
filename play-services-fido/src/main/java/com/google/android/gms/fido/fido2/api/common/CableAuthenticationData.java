/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import org.microg.safeparcel.AutoSafeParcelable;

public class CableAuthenticationData extends AutoSafeParcelable {
    @Field(1)
    private long version;
    @Field(2)
    @NonNull
    private byte[] clientEid;
    @Field(3)
    @NonNull
    private byte[] authenticatorEid;
    @Field(4)
    @NonNull
    private byte[] sessionPreKey;

    public static final Creator<CableAuthenticationData> CREATOR = new AutoCreator<>(CableAuthenticationData.class);
}
