/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetVersionParams extends AutoSafeParcelable {
    @Field(1)
    public ILongCallback callback;

    private GetVersionParams() {
    }

    public GetVersionParams(ILongCallback callback) {
        this.callback = callback;
    }

    public static final Creator<GetVersionParams> CREATOR = new AutoCreator<>(GetVersionParams.class);
}
