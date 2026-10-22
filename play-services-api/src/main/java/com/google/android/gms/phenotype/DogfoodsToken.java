/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.safeparcel.AutoSafeParcelable;

public class DogfoodsToken extends AutoSafeParcelable {
    @Field(2)
    public byte[] token;

    public DogfoodsToken() {
    }

    public DogfoodsToken(byte[] token) {
        this.token = token;
    }

    public static final Creator<DogfoodsToken> CREATOR = new AutoCreator<>(DogfoodsToken.class);
}
