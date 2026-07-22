/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class NetworkLocationStatus extends AutoSafeParcelable {
    @Field(1)
    public int wifiStatus;
    @Field(2)
    public int cellStatus;
    @Field(3)
    public long systemTimeMs;
    @Field(4)
    public long elapsedRealtimeNs;

    public NetworkLocationStatus() {
    }

    public NetworkLocationStatus(int wifiStatus, int cellStatus, long systemTimeMs, long elapsedRealtimeNs) {
        this.wifiStatus = wifiStatus;
        this.cellStatus = cellStatus;
        this.systemTimeMs = systemTimeMs;
        this.elapsedRealtimeNs = elapsedRealtimeNs;
    }

    public static final Creator<NetworkLocationStatus> CREATOR = new AutoCreator<>(NetworkLocationStatus.class);
}
