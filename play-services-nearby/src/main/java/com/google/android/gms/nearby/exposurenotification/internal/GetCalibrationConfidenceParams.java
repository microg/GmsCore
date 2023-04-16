/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetCalibrationConfidenceParams extends AutoSafeParcelable {
    @Field(1)
    public IIntCallback callback;

    private GetCalibrationConfidenceParams() {}

    public GetCalibrationConfidenceParams(IIntCallback callback) {
        this.callback = callback;
    }

    public static final Creator<GetCalibrationConfidenceParams> CREATOR = new AutoCreator<>(GetCalibrationConfidenceParams.class);
}
