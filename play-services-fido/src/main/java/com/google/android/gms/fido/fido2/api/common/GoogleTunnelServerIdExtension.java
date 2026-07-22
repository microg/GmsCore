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
public class GoogleTunnelServerIdExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getTunnelServerId")
    @NonNull
    private final String tunnelServerId;

    @Constructor
    public GoogleTunnelServerIdExtension(@Param(1) @NonNull String tunnelServerId) {
        this.tunnelServerId = tunnelServerId;
    }

    @NonNull
    public String getTunnelServerId() {
        return tunnelServerId;
    }

    @Override
    public String toString() {
        return ToStringHelper.name("GoogleTunnelServerIdExtension").field("tunnelServerId", tunnelServerId).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleTunnelServerIdExtension> CREATOR = AbstractSafeParcelable.findCreator(GoogleTunnelServerIdExtension.class);
}
