/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetStatusParams extends AutoSafeParcelable {
    @Field(1)
    public ILongCallback callback;

    private GetStatusParams() {}

    public GetStatusParams(ILongCallback callback) {
        this.callback = callback;
    }

    public static final Creator<GetStatusParams> CREATOR = new AutoCreator<>(GetStatusParams.class);
}
