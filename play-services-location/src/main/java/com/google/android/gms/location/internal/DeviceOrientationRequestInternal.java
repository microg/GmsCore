/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import com.google.android.gms.location.DeviceOrientationRequest;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class DeviceOrientationRequestInternal extends AutoSafeParcelable {

    @Field(1)
    public DeviceOrientationRequest request;

    @Field(value = 2, subClass = ClientIdentity.class)
    public List<ClientIdentity> clients;

    @Field(3)
    public String tag;

    @Override
    public String toString() {
        return "DeviceOrientationRequestInternal{" +
                "request=" + request +
                ", clients=" + clients +
                ", tag='" + tag + '\'' +
                '}';
    }

    public static final Creator<DeviceOrientationRequestInternal> CREATOR = new AutoCreator<DeviceOrientationRequestInternal>(DeviceOrientationRequestInternal.class);
}
