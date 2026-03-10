/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.facs.cache;

import org.microg.safeparcel.AutoSafeParcelable;

public class FacsCacheCallOptions extends AutoSafeParcelable {
    @Field(1)
    public String instanceId;
    @Field(2)
    public long version;

    public static final Creator<FacsCacheCallOptions> CREATOR = new AutoCreator<>(FacsCacheCallOptions.class);
}
