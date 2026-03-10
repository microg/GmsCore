/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;

import org.microg.safeparcel.AutoSafeParcelable;

public class RequestPreAuthorizedTemporaryExposureKeyReleaseParams extends AutoSafeParcelable {
    @Field(1)
    public IStatusCallback callback;

    private RequestPreAuthorizedTemporaryExposureKeyReleaseParams() {
    }

    public RequestPreAuthorizedTemporaryExposureKeyReleaseParams(IStatusCallback callback) {
        this.callback = callback;
    }

    public static final Creator<RequestPreAuthorizedTemporaryExposureKeyReleaseParams> CREATOR = new AutoCreator<>(RequestPreAuthorizedTemporaryExposureKeyReleaseParams.class);
}
