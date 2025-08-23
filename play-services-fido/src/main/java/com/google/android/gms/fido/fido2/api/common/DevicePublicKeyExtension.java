/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

@PublicApi
@SafeParcelable.Class
public class DevicePublicKeyExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "isDevicePublicKey")
    private final boolean devicePublicKey;

    @Constructor
    public DevicePublicKeyExtension(@Param(1) boolean devicePublicKey) {
        this.devicePublicKey = devicePublicKey;
    }

    public boolean isDevicePublicKey() {
        return devicePublicKey;
    }

    @Override
    public String toString() {
        return ToStringHelper.name("DevicePublicKeyExtension").field("isDevicePublicKey", devicePublicKey).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DevicePublicKeyExtension> CREATOR = AbstractSafeParcelable.findCreator(DevicePublicKeyExtension.class);
}
