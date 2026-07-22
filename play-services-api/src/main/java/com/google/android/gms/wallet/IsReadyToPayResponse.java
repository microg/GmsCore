/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet;

import org.microg.safeparcel.AutoSafeParcelable;

public class IsReadyToPayResponse extends AutoSafeParcelable {
    @Field(1)
    public boolean result;
    @Field(2)
    public String json;

    private IsReadyToPayResponse() {
    }

    public IsReadyToPayResponse(boolean result, String json) {
        this.result = result;
        this.json = json;
    }

    public static final Creator<IsReadyToPayResponse> CREATOR = new AutoCreator<>(IsReadyToPayResponse.class);
}
