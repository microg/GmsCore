/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.firstparty.dataservice;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class ClearTokenRequest extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    public String token;

    public static final Creator<ClearTokenRequest> CREATOR = findCreator(ClearTokenRequest.class);
}
