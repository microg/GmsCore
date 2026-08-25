/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class IsEnabledParams extends AutoSafeParcelable {
    @Field(2)
    public IBooleanCallback callback;

    private IsEnabledParams() {
    }

    public IsEnabledParams(IBooleanCallback callback) {
        this.callback = callback;
    }

    public static final Creator<IsEnabledParams> CREATOR = new AutoCreator<>(IsEnabledParams.class);
}
