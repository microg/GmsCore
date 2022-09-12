/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.Bundle;

import com.google.android.gms.common.Feature;

import org.microg.safeparcel.AutoSafeParcelable;

public class ConnectionInfo extends AutoSafeParcelable {
    @Field(1)
    public Bundle params;
    @Field(2)
    public Feature[] features;
    @Field(3)
    public int unknown3;

    public static final Creator<ConnectionInfo> CREATOR = new AutoSafeParcelable.AutoCreator<>(ConnectionInfo.class);
}
