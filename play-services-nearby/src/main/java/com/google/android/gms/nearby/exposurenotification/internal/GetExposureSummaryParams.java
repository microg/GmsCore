/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetExposureSummaryParams extends AutoSafeParcelable {
    @Field(2)
    public IExposureSummaryCallback callback;
    @Field(3)
    public String token;

    private GetExposureSummaryParams() {}

    public GetExposureSummaryParams(IExposureSummaryCallback callback, String token) {
        this.callback = callback;
        this.token = token;
    }

    public static final Creator<GetExposureSummaryParams> CREATOR = new AutoCreator<>(GetExposureSummaryParams.class);
}
