/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class RcsConfiguration extends AbstractSafeParcelable {
    @Field(1)
    public boolean rcsEnabled;

    @Field(2)
    public String acsUrl;

    @Field(3)
    public String provisionedMsisdn;

    @Field(4)
    public byte[] configData;

    public RcsConfiguration() {
        this.rcsEnabled = true;
    }

    public RcsConfiguration(boolean rcsEnabled, String acsUrl, String provisionedMsisdn, byte[] configData) {
        this.rcsEnabled = rcsEnabled;
        this.acsUrl = acsUrl;
        this.provisionedMsisdn = provisionedMsisdn;
        this.configData = configData;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("RcsConfiguration")
                .field("rcsEnabled", rcsEnabled)
                .field("acsUrl", acsUrl)
                .field("provisionedMsisdn", provisionedMsisdn)
                .field("configData", configData)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RcsConfiguration> CREATOR = findCreator(RcsConfiguration.class);
}
