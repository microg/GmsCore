/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.safetynet;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AttestationData extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    private String jwsResult;

    private AttestationData() {
    }

    public AttestationData(String jwsResult) {
        this.jwsResult = jwsResult;
    }

    public String getJwsResult() {
        return jwsResult;
    }

    public static final Creator<AttestationData> CREATOR = new AutoCreator<AttestationData>(AttestationData.class);
}
