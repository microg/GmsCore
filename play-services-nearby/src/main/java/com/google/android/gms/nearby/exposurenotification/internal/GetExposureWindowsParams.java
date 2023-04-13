/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetExposureWindowsParams extends AutoSafeParcelable {
    @Field(1)
    public IExposureWindowListCallback callback;
    @Field(2)
    public String token;

    private GetExposureWindowsParams() {}

    public GetExposureWindowsParams(IExposureWindowListCallback callback, String token) {
        this.callback = callback;
        this.token = token;
    }

    public static final Creator<GetExposureWindowsParams> CREATOR = new AutoCreator<>(GetExposureWindowsParams.class);
}
