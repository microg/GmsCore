/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens;

import org.microg.safeparcel.AutoSafeParcelable;

public class PoToken extends AutoSafeParcelable {

    @Field(1)
    public byte[] data;

    public PoToken(byte[] data) {
        this.data = data;
    }

    public static Creator<PoToken> CREATOR = findCreator(PoToken.class);
}
