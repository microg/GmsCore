/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetTemporaryExposureKeyHistoryParams extends AutoSafeParcelable {
    @Field(2)
    public ITemporaryExposureKeyListCallback callback;

    private GetTemporaryExposureKeyHistoryParams() {}

    public GetTemporaryExposureKeyHistoryParams(ITemporaryExposureKeyListCallback callback) {
        this.callback = callback;
    }

    public static final Creator<GetTemporaryExposureKeyHistoryParams> CREATOR = new AutoCreator<>(GetTemporaryExposureKeyHistoryParams.class);
}
