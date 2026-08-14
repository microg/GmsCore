/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import com.google.android.gms.nearby.exposurenotification.DailySummariesConfig;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetDailySummariesParams extends AutoSafeParcelable {
    @Field(1)
    public IDailySummaryListCallback callback;
    @Field(2)
    public DailySummariesConfig config;

    private GetDailySummariesParams() {}

    public GetDailySummariesParams(IDailySummaryListCallback callback, DailySummariesConfig config) {
        this.callback = callback;
        this.config = config;
    }

    public static final Creator<GetDailySummariesParams> CREATOR = new AutoCreator<>(GetDailySummariesParams.class);
}
