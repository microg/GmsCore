/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.gmscompliance;

import android.app.PendingIntent;

import org.microg.safeparcel.AutoSafeParcelable;

public class GmsDeviceComplianceResponse extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    public boolean compliant;
    @Field(3)
    public PendingIntent errorIntent;

    public static final Creator<GmsDeviceComplianceResponse> CREATOR = new AutoCreator<>(GmsDeviceComplianceResponse.class);
}
