/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class AppMetadata extends AutoSafeParcelable {
    @Field(2)
    public String packageName;
    @Field(4)
    public String versionName;
    @Field(11)
    public long versionCode;

    public static final Creator<AppMetadata> CREATOR = new AutoCreator<>(AppMetadata.class);
}
