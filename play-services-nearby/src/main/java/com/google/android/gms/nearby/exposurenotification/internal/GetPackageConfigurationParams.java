/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetPackageConfigurationParams extends AutoSafeParcelable {
    @Field(1)
    public IPackageConfigurationCallback callback;

    private GetPackageConfigurationParams() {}

    public GetPackageConfigurationParams(IPackageConfigurationCallback callback) {
        this.callback = callback;
    }

    public static final Creator<GetPackageConfigurationParams> CREATOR = new AutoCreator<>(GetPackageConfigurationParams.class);
}
