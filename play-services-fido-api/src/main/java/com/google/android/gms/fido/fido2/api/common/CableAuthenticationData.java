/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.safeparcel.AutoSafeParcelable;

public class CableAuthenticationData extends AutoSafeParcelable {
    @Field(1)
    private long version;
    @Field(2)
    private byte[] clientEid;
    @Field(3)
    private byte[] authenticatorEid;
    @Field(4)
    private byte[] sessionPreKey;

    public static final Creator<CableAuthenticationData> CREATOR = new AutoCreator<>(CableAuthenticationData.class);
}
