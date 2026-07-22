/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;

import org.microg.safeparcel.AutoSafeParcelable;

public class StartParams extends AutoSafeParcelable {
    @Field(3)
    public IStatusCallback callback;
    @Field(4)
    public ExposureConfiguration configuration;

    private StartParams() {
    }

    public StartParams(IStatusCallback callback) {
        this.callback = callback;
    }

    public static final Creator<StartParams> CREATOR = new AutoCreator<>(StartParams.class);
}
