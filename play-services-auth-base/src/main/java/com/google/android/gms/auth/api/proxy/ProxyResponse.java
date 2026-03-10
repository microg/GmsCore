/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.proxy;

import android.app.PendingIntent;
import android.os.Bundle;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class ProxyResponse extends AutoSafeParcelable {
    public static final int STATUS_CODE_NO_CONNECTION = -1;

    @Field(1000)
    private int versionCode = 1;
    @Field(1)
    public int gmsStatusCode;
    @Field(2)
    public PendingIntent recoveryAction;
    @Field(3)
    public int httpStatusCode;
    @Field(4)
    public Bundle headers;
    @Field(5)
    public byte[] body;

    public static final Creator<ProxyResponse> CREATOR = new AutoCreator<>(ProxyResponse.class);
}
