/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetExposureInformationParams extends AutoSafeParcelable {
    @Field(2)
    public IExposureInformationListCallback callback;
    @Field(3)
    public String token;

    private GetExposureInformationParams() {}

    public GetExposureInformationParams(IExposureInformationListCallback callback, String token) {
        this.callback = callback;
        this.token = token;
    }

    public static final Creator<GetExposureInformationParams> CREATOR = new AutoCreator<>(GetExposureInformationParams.class);
}
